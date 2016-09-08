package org.metaborg.spt.core.extract.expectations;

import java.util.List;

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

    // AnalyzeMessages(Some(<operation>), <number>, <severity>, Some(AtPart([SelectionRef(<i>), SelectionRef(<j>)])))
    private static final String CONS = "AnalyzeMessages";
    // AnalyzeMessagePattern(severity, content, optional at part)
    private static final String LIKE = "AnalyzeMessagePattern";
    private static final String ERR = "Error";
    private static final String WARN = "Warning";
    private static final String NOTE = "Note";
    private static final String EQ = "Equal";
    private static final String LT = "Less";
    private static final String LE = "LessOrEqual";
    private static final String MT = "More";
    private static final String ME = "MoreOrEqual";
    private static final String NONE = "None";

    private final ISpoofaxTracingService traceService;

    @Inject public AnalyzeExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final String cons = SPTUtil.consName(expectationTerm);
        switch(cons) {
            case CONS:
                return expectationTerm.getSubtermCount() == 4 && Term.isTermInt(expectationTerm.getSubterm(1));
            case LIKE:
                return expectationTerm.getSubtermCount() == 3 && Term.isTermString(expectationTerm.getSubterm(1));
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

    private MessageSeverity getSeverity(IStrategoTerm sevTerm) {
        final String sevStr = SPTUtil.consName(sevTerm);
        switch(sevStr) {
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

    private List<Integer> getSelections(IStrategoTerm optionalAtPart) {
        final List<Integer> selections = Lists.newArrayList();
        if(!NONE.equals(SPTUtil.consName(optionalAtPart))) {
            // Some(AtPart([SelectionRef(i), ...]))
            IStrategoList list = (IStrategoList) optionalAtPart.getSubterm(0).getSubterm(0);
            for(IStrategoTerm sel : list) {
                selections.add(Term.asJavaInt(sel));
            }
        }
        return selections;
    }

    private ISourceRegion getRegion(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final ISourceLocation loc = traceService.location(expectationTerm);
        return loc == null ? inputFragment.getRegion() : loc.region();
    }

    private AnalysisMessageExpectation getLikeExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        // get the severity
        final IStrategoTerm sevTerm = expectationTerm.getSubterm(0);
        final MessageSeverity severity = getSeverity(sevTerm);

        // get the contents that should be part of the message
        final String content = Term.asJavaString(expectationTerm.getSubterm(1));

        // get the selections from the 'at' part
        final IStrategoTerm optionalAtPart = expectationTerm.getSubterm(2);
        final List<Integer> selections = getSelections(optionalAtPart);

        // get the region
        final ISourceRegion region = getRegion(inputFragment, expectationTerm);

        // we expect at least 1 message with the given content
        return new AnalysisMessageExpectation(region, 1, severity, selections, Operation.MORE_OR_EQUAL, content);
    }

    private AnalysisMessageExpectation getNumCheckExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        // get the severity
        final IStrategoTerm sevTerm = expectationTerm.getSubterm(2);
        final MessageSeverity severity = getSeverity(sevTerm);

        // get the selections from the 'at' part
        final IStrategoTerm optionalAtPart = expectationTerm.getSubterm(3);
        final List<Integer> selections = getSelections(optionalAtPart);

        // get the operation (defaults to 'equal')
        final IStrategoTerm optionalOpt = expectationTerm.getSubterm(0);
        final Operation opt;
        if(NONE.equals(SPTUtil.consName(optionalOpt))) {
            opt = Operation.EQUAL;
        } else {
            final String optStr = SPTUtil.consName(optionalOpt.getSubterm(0));
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
        return new AnalysisMessageExpectation(region, Term.asJavaInt(expectationTerm.getSubterm(1)), severity,
            selections, opt, null);
    }
}
