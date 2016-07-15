package org.metaborg.spt.core.run;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.core.unit.ParseContrib;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.client.imploder.Tokenizer;

import com.google.common.collect.Lists;
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
        ITokenizer itokenizer = ImploderAttachment.getTokenizer(ast);
        if(itokenizer == null) {
            logger.warn("Found a fragment with no tokenizer! Can't update the offsets. \"{}\"", textStr);
            return p;
        }

        // adjust the tokens for each piece of the fragment
        // this makes NO assumptions about the order of the startOffsets of the token stream
        // it DOES assume that the pieces of text of the fragment are ordered based on the correct order of text
        int[] startOffsets = new int[itokenizer.getTokenCount()];
        int[] endOffsets = new int[itokenizer.getTokenCount()];
        int currStartOffsetOfPiece = 0;
        int currEndOffsetOfPiece = 0;
        for(FragmentPiece piece : fragmentPieces) {
            int pieceLength = piece.text.length();
            currEndOffsetOfPiece = currStartOffsetOfPiece + pieceLength - 1;
            int adjustment = piece.startOffset - currStartOffsetOfPiece;
            for(IToken itoken : itokenizer) {
                int startOffset = itoken.getStartOffset();
                if(startOffset >= currStartOffsetOfPiece && startOffset <= currEndOffsetOfPiece) {
                    Token token = (Token) itoken;
                    startOffsets[token.getIndex()] = startOffset + adjustment;
                    endOffsets[token.getIndex()] = token.getEndOffset() + adjustment;
                }
            }
            currStartOffsetOfPiece += pieceLength;
        }

        // Do post processing to ensure the token stream is ordered by offsets again
        final List<Token> tokens = Lists.newArrayList();
        Token eof = null;
        for(IToken itoken : itokenizer) {
            if(IToken.TK_EOF == itoken.getKind()) {
                eof = (Token) itoken;
            } else {
                Token token = (Token) itoken;
                token.setStartOffset(startOffsets[token.getIndex()]);
                token.setEndOffset(endOffsets[token.getIndex()]);
                tokens.add(token);
            }
        }
        Collections.sort(tokens, new Comparator<IToken>() {
            @Override public int compare(IToken o1, IToken o2) {
                int r = o1.getStartOffset() - o2.getStartOffset();
                if(r == 0) {
                    r = o1.getEndOffset() - o2.getEndOffset();
                }
                return r;
            }
        });
        int lastOffset = tokens.get(tokens.size() - 1).getEndOffset();
        eof.setStartOffset(lastOffset + 1);
        eof.setEndOffset(lastOffset);
        tokens.add(eof);

        Tokenizer newTokenizer = new Tokenizer(itokenizer.getInput(), itokenizer.getFilename(), null, false);
        for(Token token : tokens) {
            // NOTE: this will break if run with assertions turned on
            // but as this entire approach is one big hack, I don't really care at the moment
            newTokenizer.reassignToken(token);
        }
        newTokenizer.setAst(ast);
        newTokenizer.initAstNodeBinding();

        // now the offsets of the tokens are updated
        // changing the state like this should update the offsets of the ast nodes automatically
        // but next, we need to update the offsets of the parse messages
        List<IMessage> changedMessages = Lists.newLinkedList();
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
            new ParseContrib(p.valid(), p.success(), p.ast(), changedMessages, p.duration()));
    }

    @Override public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl language, ILanguageImpl dialect,
        IFragmentParserConfig config) throws ParseException {
        if(config == null || !(config instanceof ISpoofaxFragmentParserConfig)) {
            return parse(fragment, language, dialect, (ISpoofaxFragmentParserConfig) null);
        } else {
            return parse(fragment, language, dialect, (ISpoofaxFragmentParserConfig) config);
        }
    }

}
