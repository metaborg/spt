package org.metaborg.spt.core.run;

import java.util.*;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.source.SourceRegion;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.IFragment.FragmentPiece;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.*;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.*;
import org.spoofax.terms.visitor.AStrategoTermVisitor;
import org.spoofax.terms.visitor.StrategoTermVisitee;

import com.google.inject.Inject;

/**
 * Parser for fragments languages.
 * 
 * Ensures the correct offsets of the parse result by post processing the parse result and updating origins. Updating
 * origins happens by updating the offsets of the tokens of the AST's tokenizer. This requires quite heavy knowledge of
 * the Spoofax internals, so we could use an API for changing origin locations and not just querying them.
 */
public class SpoofaxOriginFragmentParser implements ISpoofaxFragmentParser {

    private static final ILogger logger = LoggerUtils.logger(SpoofaxOriginFragmentParser.class);

    private final ISpoofaxInputUnitService inputService;
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxSyntaxService parseService;

    @Inject public SpoofaxOriginFragmentParser(ISpoofaxInputUnitService inputService, ISpoofaxUnitService unitService,
        ISpoofaxSyntaxService parseService) {
        this.inputService = inputService;
        this.unitService = unitService;
        this.parseService = parseService;
    }

    @Override public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl language,
        @Nullable ILanguageImpl dialect, @Nullable ISpoofaxFragmentParserConfig config) throws ParseException {

        // record the text of the fragment
        final Iterable<FragmentPiece> fragmentPieces = fragment.getText();
        StringBuilder textBuilder = new StringBuilder();
        for(FragmentPiece piece : fragmentPieces) {
            textBuilder.append(piece.text);
        }
        final String textStr = textBuilder.toString();


        // now we can parse the fragment
        final ISpoofaxInputUnit input;
        JSGLRParserConfiguration pConfig = null;
        if(config != null) {
            pConfig = config.getParserConfigForLanguage(language);
        }

        if(pConfig == null) {
            input = inputService.inputUnit(fragment.getResource(), textStr, language, dialect);
        } else {
            input = inputService.inputUnit(fragment.getResource(), textStr, language, dialect, pConfig);
        }

        ISpoofaxParseUnit p = parseService.parse(input);

        // short circuit if there was no result
        if(!p.valid()) {
            return p;
        }
        IStrategoTerm ast = p.ast();
        if(ast == null) {
            return p;
        }

        // start changing the offsets by changing the offsets of the tokens
        ITokens originalTokens = ImploderAttachment.getTokenizer(ast);
        if(originalTokens == null) {
            logger.warn("Found a fragment with no tokenizer! Can't update the offsets. \"{}\"", textStr);
            return p;
        }

        // adjust the tokens for each piece of the fragment
        // this makes NO assumptions about the order of the startOffsets of the token stream
        // it DOES assume that the pieces of text of the fragment are ordered based on the correct order of text
        Map<IToken, Integer> startOffsets = new HashMap<>(originalTokens.getTokenCount());
        Map<IToken, Integer> endOffsets = new HashMap<>(originalTokens.getTokenCount());
        IToken eof = null;
        int currStartOffsetOfPiece = 0;
        int currEndOffsetOfPiece = 0;
        for(FragmentPiece piece : fragmentPieces) {
            int pieceLength = piece.text.length();
            currEndOffsetOfPiece = currStartOffsetOfPiece + pieceLength - 1;
            int adjustment = piece.startOffset - currStartOffsetOfPiece;
            for(IToken token : originalTokens.allTokens()) {
                int startOffset = token.getStartOffset();
                if(startOffset >= currStartOffsetOfPiece && startOffset <= currEndOffsetOfPiece) {
                    startOffsets.put(token, startOffset + adjustment);
                    endOffsets.put(token, token.getEndOffset() + adjustment);
                }
                if(token.getKind() == IToken.Kind.TK_EOF) {
                    eof = token;
                }
            }
            currStartOffsetOfPiece += pieceLength;
        }

        // Only post process tokens when there are tokens, and when there is an end-of-file token.
        if(!startOffsets.isEmpty() && eof != null) {
            MappingTokenizer newTokenizer = new MappingTokenizer(originalTokens);
            for(IToken token : originalTokens.allTokens()) {
                if(token.getKind() == IToken.Kind.TK_EOF) {
                    int lastOffset = newTokenizer.tokens.get(newTokenizer.tokens.size() - 1).getEndOffset();
                    newTokenizer.addToken(lastOffset + 1, lastOffset, eof);
                } else {
                    newTokenizer.addToken(startOffsets.get(token), endOffsets.get(token), token);
                }
            }
            newTokenizer.overwriteAttachments(ast);
        }

        // now the offsets of the tokens are updated
        // changing the state like this should update the offsets of the ast nodes automatically
        // but next, we need to update the offsets of the parse messages
        List<IMessage> changedMessages = new LinkedList<>();
        for(IMessage m : p.messages()) {
            ISourceRegion region = m.region();
            if(region == null) {
                continue;
            }
            int newStart = region.startOffset();
            int newEnd = region.endOffset();
            int offset = 0;
            for(FragmentPiece piece : fragmentPieces) {
                int startOffset = region.startOffset();
                int pieceEndExclusive = offset + piece.text.length();
                if(startOffset >= offset && startOffset < pieceEndExclusive) {
                    newStart = piece.startOffset + (startOffset - offset);
                }
                int endOffset = region.endOffset();
                if(endOffset >= offset && endOffset < pieceEndExclusive) {
                    newEnd = piece.startOffset + (endOffset - offset);
                }
                offset += piece.text.length();
            }
            if(newStart != region.startOffset() || newEnd != region.endOffset()) {
                ISourceRegion newRegion = new SourceRegion(newStart, newEnd);
                changedMessages.add(MessageUtil.setRegion(m, newRegion));
            }
        }
        return unitService.parseUnit(input,
            new ParseContrib(p.valid(), p.success(), p.isAmbiguous(), p.ast(), changedMessages, p.duration()));
    }

    @Override public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl language, ILanguageImpl dialect,
        IFragmentParserConfig config) throws ParseException {
        if(!(config instanceof ISpoofaxFragmentParserConfig)) {
            return parse(fragment, language, dialect, (ISpoofaxFragmentParserConfig) null);
        } else {
            return parse(fragment, language, dialect, (ISpoofaxFragmentParserConfig) config);
        }
    }

    private static class MappingTokenizer implements ITokens {

        private final List<IToken> tokens = new ArrayList<>();
        private final Map<IToken, IToken> oldToNewTokens = new HashMap<>();
        private final Map<IToken, IToken> newToOldTokens = new HashMap<>();
        private final String input;
        private final String filename;

        private MappingTokenizer(ITokens originalTokens) {
            this.input = originalTokens.getInput();
            this.filename = originalTokens.getFilename();
        }

        private void addToken(int startOffset, int endOffset, IToken originalToken) {
            Token newToken = new MappedToken(this, startOffset, endOffset, originalToken);
            newToken.setAstNode(originalToken.getAstNode());
            tokens.add(newToken);
            oldToNewTokens.put(originalToken, newToken);
            newToOldTokens.put(newToken, originalToken);
        }

        private void overwriteAttachments(IStrategoTerm ast) {
            StrategoTermVisitee.topdown(new AStrategoTermVisitor() {
                @Override public boolean visit(IStrategoTerm term) {
                    ImploderAttachment originalAttachment = ImploderAttachment.get(term);

                    // For incremental parsing, the reused AST nodes already have updated ImploderAttachments with new
                    // MappedTokens. In this case, we should get the original token to index the oldToNewTokens Map,
                    // because the offsets might be updated since the previous version.
                    IToken leftToken = oldToNewTokens.get(originalAttachment.getLeftToken() instanceof MappedToken
                        ? ((MappedToken) originalAttachment.getLeftToken()).originalToken
                        : originalAttachment.getLeftToken());
                    IToken rightToken = oldToNewTokens.get(originalAttachment.getRightToken() instanceof MappedToken
                        ? ((MappedToken) originalAttachment.getRightToken()).originalToken
                        : originalAttachment.getRightToken());

                    ImploderAttachment.putImploderAttachment(term, term instanceof ListImploderAttachment,
                        originalAttachment.getSort(), leftToken, rightToken, originalAttachment.isBracket(),
                        originalAttachment.isCompletion(), originalAttachment.isNestedCompletion(),
                        originalAttachment.isSinglePlaceholderCompletion());

                    return true;
                }
            }, ast);
        }

        @Override public String getInput() {
            return input;
        }

        @Override public int getTokenCount() {
            return tokens.size();
        }

        @Override public IToken getTokenAtOffset(int offset) {
            for(IToken token : tokens) {
                if(token.getStartOffset() == offset)
                    return token;
            }
            return null;
        }

        @Override public String getFilename() {
            return filename;
        }

        @Override public String toString(IToken left, IToken right) {
            return toString(newToOldTokens.get(left).getStartOffset(), newToOldTokens.get(right).getEndOffset());
        }

        @Override public String toString(int startOffset, int endOffset) {
            return input.substring(startOffset, endOffset);
        }

        @Override public Iterator<IToken> iterator() {
            return new Tokenizer.AmbiguousToNonAmbiguousIterator(allTokens());
        }

        @Override public Iterable<IToken> allTokens() {
            return Collections.unmodifiableList(tokens);
        }
    }

    private static class MappedToken extends Token {
        private final IToken originalToken;

        public MappedToken(ITokens tokens, int startOffset, int endOffset, IToken originalToken) {
            super(tokens, tokens.getFilename(), -1, -1, -1, startOffset, endOffset, originalToken.getKind());
            this.originalToken = originalToken;
        }
    }

}
