package org.metaborg.mbt.core.run.expectations;

import java.util.Collection;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.context.IContext;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.NotExpectation;
import org.metaborg.mbt.core.run.IExpectationEvaluatorService;
import org.metaborg.mbt.core.run.ITestExpectationEvaluator;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.mbt.core.run.ITestExpectationOutput;
import org.metaborg.mbt.core.run.TestExpectationOutput;

import com.google.inject.Inject;

public class NotExpectationEvaluator<P extends IParseUnit, A extends IAnalyzeUnit>
    implements ITestExpectationEvaluator<P, A, NotExpectation> {

    private final IExpectationEvaluatorService<P, A> evaluatorService;


    @Inject public NotExpectationEvaluator(IExpectationEvaluatorService<P, A> evaluatorService) {
        this.evaluatorService = evaluatorService;
    }


    @Override public Collection<Integer> usesSelections(IFragment fragment, NotExpectation expectation) {
        final ITestExpectation subExpectation = expectation.subExpectation;
        final ITestExpectationEvaluator<P, A, ITestExpectation> subEvaluator = evaluatorService.lookup(subExpectation);
        return subEvaluator.usesSelections(fragment, subExpectation);
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, NotExpectation expectation) {
        final ITestExpectation subExpectation = expectation.subExpectation;
        final ITestExpectationEvaluator<P, A, ITestExpectation> subEvaluator = evaluatorService.lookup(subExpectation);
        return subEvaluator.getPhase(languageUnderTestCtx, subExpectation);
    }

    @Override public ITestExpectationOutput<P, A> evaluate(ITestExpectationInput<P, A> input,
        NotExpectation expectation) {
        final ITestExpectation subExpectation = expectation.subExpectation;
        final ITestExpectationEvaluator<P, A, ITestExpectation> subEvaluator = evaluatorService.lookup(subExpectation);
        final ITestExpectationOutput<P, A> result = subEvaluator.evaluate(input, subExpectation);
        return new TestExpectationOutput<P, A>(!result.isSuccessful(), result.getMessages(),
            result.getFragmentResults());
    }
}
