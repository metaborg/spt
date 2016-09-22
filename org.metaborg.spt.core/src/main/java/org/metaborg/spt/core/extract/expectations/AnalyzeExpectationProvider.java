package org.metaborg.spt.core.extract.expectations;

import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.AnalysisMessageExpectation;
import org.metaborg.mbt.core.model.expectations.AnalysisMessageExpectation.Operation;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.common.collect.Lists;
import com.google.inject.Inject;


public class AnalyzeExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final ILogger logger = LoggerUtils.logger(AnalyzeExpectationProvider.class);

    // AnalyzeMessages(Some(<operation>), <number>, <severity>, Some(AtPart([<i>, <j>])))
    private static final String CONS = "AnalyzeMessages";
    // AnalyzeMessagePattern(severity, content, optional at part)
    private static final String LIKE = "AnalyzeMessagePattern";

    private static final String AT_PART = "AtPart";

    private static final String ERR = "Error";
    private static final String WARN = "Warning";
    private static final String NOTE = "Note";
    private static final String EQ = "Equal";
    private static final String LT = "Less";
    private static final String LE = "LessOrEqual";
    private static final String MT = "More";
    private static final String ME = "MoreOrEqual";

    private final ISpoofaxTracingService traceService;

    @Inject public AnalyzeExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final String cons = SPTUtil.consName(expectationTerm);
        switch(cons) {
            case CONS:
                // this is a check for the number of messages of a given severity
                return NumCheck.check(expectationTerm);
            case LIKE:
                // this is a check for the content of messages
                return LikeCheck.check(expectationTerm);
            default:
                return false;
        }
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        logger.debug("Creating an expectation object for {}", expectationTerm);
        final String cons = SPTUtil.consName(expectationTerm);
        switch(cons) {
            case CONS:
                return getNumCheckExpectation(inputFragment, expectationTerm);
            case LIKE:
                return getLikeExpectation(inputFragment, expectationTerm);
            default:
                throw new IllegalArgumentException(
                    "This provider never claimed to be able to handle a " + expectationTerm);
        }
    }

    private ISourceRegion getRegion(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final ISourceLocation loc = traceService.location(expectationTerm);
        return loc == null ? inputFragment.getRegion() : loc.region();
    }

    private AnalysisMessageExpectation getLikeExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        // get the severity
        final IStrategoTerm sevTerm = LikeCheck.getSeverityTerm(expectationTerm);
        final MessageSeverity severity = getSeverity(sevTerm);

        // get the contents that should be part of the message
        final String content = Term.asJavaString(LikeCheck.getContentTerm(expectationTerm));

        // get the selections from the 'at' part
        final IStrategoTerm optionalAtPart = LikeCheck.getOptionalAtPartTerm(expectationTerm);
        final List<Integer> selections = getSelections(optionalAtPart);

        // get the region
        final ISourceRegion region = getRegion(inputFragment, expectationTerm);

        // we expect at least 1 message with the given content
        return new AnalysisMessageExpectation(region, 1, severity, selections, Operation.MORE_OR_EQUAL, content);
    }

    private AnalysisMessageExpectation getNumCheckExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        // get the severity
        final IStrategoTerm sevTerm = NumCheck.getSeverityTerm(expectationTerm);
        final MessageSeverity severity = getSeverity(sevTerm);

        // get the selections from the 'at' part
        final IStrategoTerm optionalAtPart = NumCheck.getOptionalAtPartTerm(expectationTerm);
        final List<Integer> selections = getSelections(optionalAtPart);

        // get the operation (defaults to 'equal')
        final @Nullable IStrategoTerm optTerm =
            SPTUtil.getOptionValue(NumCheck.getOptionalOperatorTerm(expectationTerm));
        final Operation opt;
        if(optTerm == null) {
            // it was a None()
            opt = Operation.EQUAL;
        } else {
            // it was a Some(optTerm)
            final String optStr = SPTUtil.consName(optTerm);
            switch(optStr) {
                case EQ:
                    opt = Operation.EQUAL;
                    break;
                case LT:
                    opt = Operation.LESS;
                    break;
                case LE:
                    opt = Operation.LESS_OR_EQUAL;
                    break;
                case MT:
                    opt = Operation.MORE;
                    break;
                case ME:
                    opt = Operation.MORE_OR_EQUAL;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "This test expectation provider can't evaluate messages with operator " + optStr);
            }
        }

        // get the region
        final ISourceRegion region = getRegion(inputFragment, expectationTerm);

        return new AnalysisMessageExpectation(region, Term.asJavaInt(NumCheck.getNumTerm(expectationTerm)), severity,
            selections, opt, null);
    }



    // AnalyzeMessages(Some(<operation>), <number>, <severity>, optional AtPart) expectation
    private static class NumCheck {

        public static IStrategoTerm getOptionalOperatorTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(0);
        }

        public static IStrategoTerm getNumTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(1);
        }

        public static IStrategoTerm getSeverityTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(2);
        }

        public static IStrategoTerm getOptionalAtPartTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(3);
        }

        // check if the given term is an optional operator that we recognize
        public static boolean checkOptionalOperator(IStrategoTerm optOp) {
            if(!SPTUtil.checkOption(optOp)) {
                return false;
            }
            final IStrategoTerm op = SPTUtil.getOptionValue(optOp);
            if(op == null) {
                // it was a None()
                return true;
            }
            switch(SPTUtil.consName(op)) {
                case EQ:
                case LT:
                case LE:
                case MT:
                case ME:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Check if the given term is an AnalyzeMessages term that we can handle.
         */
        public static boolean check(IStrategoTerm numCheck) {
            // AnalyzeMessages(optional opt, num, severity, optional atpart)
            if(!CONS.equals(SPTUtil.consName(numCheck)) || numCheck.getSubtermCount() != 4) {
                return false;
            }
            if(!checkOptionalOperator(getOptionalOperatorTerm(numCheck))) {
                return false;
            }
            if(!Term.isTermInt(getNumTerm(numCheck))) {
                return false;
            }
            if(!checkSeverity(getSeverityTerm(numCheck))) {
                return false;
            }
            if(!checkOptionalAtPart(getOptionalAtPartTerm(numCheck))) {
                return false;
            }
            return true;
        }
    }

    // AnalyzeMessagePattern(severity, content, optional at part)
    private static class LikeCheck {
        public static IStrategoTerm getSeverityTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(0);
        }

        public static IStrategoTerm getContentTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(1);
        }

        public static IStrategoTerm getOptionalAtPartTerm(IStrategoTerm expectationTerm) {
            return expectationTerm.getSubterm(2);
        }

        /**
         * Check if the given term is an AnalyzeMessagePattern term that we can handle.
         */
        public static boolean check(IStrategoTerm expectationTerm) {
            // AnalyzeMessagePattern(severity, content, optional at part)
            if(!LIKE.equals(SPTUtil.consName(expectationTerm)) || expectationTerm.getSubtermCount() != 3) {
                return false;
            }
            if(!checkSeverity(getSeverityTerm(expectationTerm))) {
                return false;
            }
            if(!Term.isTermString(getContentTerm(expectationTerm))) {
                return false;
            }
            if(!checkOptionalAtPart(getOptionalAtPartTerm(expectationTerm))) {
                return false;
            }
            return true;
        }
    }

    private static IStrategoTerm getAtPartSelectionsTerm(IStrategoTerm atPart) {
        return atPart.getSubterm(0);
    }

    // check if the given term is a valid severity
    private static boolean checkSeverity(IStrategoTerm sev) {
        switch(SPTUtil.consName(sev)) {
            case ERR:
            case WARN:
            case NOTE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Convert the term into a MessageSeverity.
     */
    private MessageSeverity getSeverity(IStrategoTerm sevTerm) {
        switch(SPTUtil.consName(sevTerm)) {
            case ERR:
                return MessageSeverity.ERROR;
            case WARN:
                return MessageSeverity.WARNING;
            case NOTE:
                return MessageSeverity.NOTE;
            default:
                throw new IllegalArgumentException(
                    "This test expectation provider can't evaluate messages of severity " + sevTerm);
        }
    }

    // AtPart([<i>, <j>, ...])
    private static boolean checkOptionalAtPart(IStrategoTerm optAtPart) {
        if(!SPTUtil.checkOption(optAtPart)) {
            return false;
        }
        final IStrategoTerm atPart = SPTUtil.getOptionValue(optAtPart);
        if(atPart == null) {
            // None()
            return true;
        } else {
            // Some(atPart)
            if(!AT_PART.equals(SPTUtil.consName(atPart)) || atPart.getSubtermCount() != 1) {
                return false;
            }

            // check list of selections
            final IStrategoTerm selections = getAtPartSelectionsTerm(atPart);
            if(!Term.isTermList(selections)) {
                return false;
            }
            final IStrategoList selectionsList = (IStrategoList) selections;
            for(IStrategoTerm selectionRef : selectionsList) {
                // should be an int
                if(!Term.isTermInt(selectionRef)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Get the numbers of the selections from an optional AtPart term.
     */
    private List<Integer> getSelections(IStrategoTerm optionalAtPart) {
        final List<Integer> selections = Lists.newArrayList();
        final IStrategoTerm atPart = SPTUtil.getOptionValue(optionalAtPart);
        if(atPart != null) {
            // Some(AtPart([SelectionRef(i), ...]))
            final IStrategoList list = (IStrategoList) getAtPartSelectionsTerm(atPart);
            for(IStrategoTerm sel : list) {
                selections.add(Term.asJavaInt(sel));
            }
        }
        return selections;
    }

}
