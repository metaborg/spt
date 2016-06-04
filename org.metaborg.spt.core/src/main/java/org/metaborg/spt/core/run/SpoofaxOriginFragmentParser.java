package org.metaborg.spt.core.run;

import java.util.Iterator;
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

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Parser for fragments of non-layout sensitive languages.
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

        final Iterable<FragmentPiece> fragmentPieces = fragment.getText();
        StringBuilder textBuilder = new StringBuilder();
        final List<OffsetAdjustment> gaps = Lists.newLinkedList();

        // record the text and the gaps in the text
        int textOffset = 0;
        int adjustedOffset = 0;
        for(FragmentPiece piece : fragmentPieces) {
            if(piece.startOffset > adjustedOffset) {
                // there is a gap between the last piece and this piece
                adjustedOffset = piece.startOffset;
                gaps.add(new OffsetAdjustment(textOffset, adjustedOffset - textOffset));
            } else if(piece.startOffset < adjustedOffset) {
                // this doesn't make any sense, the pieces should be ordered by offset.
                throw new IllegalStateException(
                    String.format("Can't process a fragment with inconsistent fragment pieces at (%1$s, %2$s).",
                        piece.startOffset, piece.text));
            }
            adjustedOffset += piece.text.length();
            textOffset += piece.text.length();
            textBuilder.append(piece.text);
        }

        // now we can parse the fragment
        final ISpoofaxInputUnit input;
        JSGLRParserConfiguration pConfig = null;
        if(config != null) {
            pConfig = config.getParserConfigForLanguage(language);
        }
        if(pConfig == null) {
            input = inputService.inputUnit(fragment.getResource(), textBuilder.toString(), language, dialect);
        } else {
            input = inputService.inputUnit(fragment.getResource(), textBuilder.toString(), language, dialect, pConfig);
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
            logger.warn("Found a fragment with no tokenizer! Can't update the offsets. \"{}\"", textBuilder);
            return p;
        }

        Iterator<OffsetAdjustment> gapIt = gaps.iterator();
        if(!gapIt.hasNext()) {
            return p;
        }

        OffsetAdjustment nextGap = gapIt.next();
        int oldAdjustment = 0;
        for(IToken itoken : itokenizer) {
            if(!(itoken instanceof Token)) {
                logger.error(
                    "Unable to properly update the offsets of the fragment, as it has an unknown token implementation ({}). Fragment: \"{}\"",
                    itoken.getClass(), textBuilder);
                return p;
            }

            final int adjustment;
            if(nextGap == null || itoken.getStartOffset() < nextGap.normalOffset) {
                adjustment = oldAdjustment;
            } else {
                // update to the next gap and record the adjustment to use at the moment
                while(nextGap != null && itoken.getStartOffset() >= nextGap.normalOffset) {
                    oldAdjustment = nextGap.adjustment;
                    nextGap = gapIt.hasNext() ? gapIt.next() : null;
                }
                adjustment = oldAdjustment;
            }
            if(adjustment > 0) {
                ((Token) itoken).setStartOffset(itoken.getStartOffset() + adjustment);
                ((Token) itoken).setEndOffset(itoken.getEndOffset() + adjustment);
            }
        }

        // now the offsets of the tokens are updated
        // changing the state like this should update the offsets of the ast nodes automatically
        // but next, we need to update the offsets of the parse messages
        List<IMessage> changedMessages = Lists.newLinkedList();
        for(IMessage m : p.messages()) {
            ISourceRegion region = m.region();
            if(region == null) {
                continue;
            }
            int startAdjustment = 0;
            int endAdjustment = 0;
            for(OffsetAdjustment gap : gaps) {
                if(gap.normalOffset <= region.startOffset()) {
                    startAdjustment = gap.adjustment;
                }
                if(gap.normalOffset <= region.endOffset()) {
                    endAdjustment = gap.adjustment;
                } else {
                    break;
                }
            }
            if(startAdjustment > 0 || endAdjustment > 0) {
                ISourceRegion newRegion =
                    new SourceRegion(region.startOffset() + startAdjustment, region.endOffset() + endAdjustment);
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

    /**
     * Represents a gap in the fragment text.
     * 
     * This means that any text at or after the normalOffset should be updated to be at normalOffset + adjustment.
     */
    private static class OffsetAdjustment {
        // the offset within the text, starting at 0
        public final int normalOffset;
        // the adjustment that has to be added to the normalOffset
        public final int adjustment;

        public OffsetAdjustment(int offset, int adjustment) {
            this.normalOffset = offset;
            this.adjustment = adjustment;
        }
    }

}
