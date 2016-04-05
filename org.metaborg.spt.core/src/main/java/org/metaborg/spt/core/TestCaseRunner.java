package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spt.core.ITestCase.ExpectationPair;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class TestCaseRunner implements ITestCaseRunner {

    private final IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService;
    private final IContextService contextService;
    private final IFragmentParser fragmentParser;

    @Inject public TestCaseRunner(Set<ITestExpectation> expectations,
        IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService, IContextService contextService,
        IFragmentParser fragmentParser) {
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.fragmentParser = fragmentParser;
    }

    public ITestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest) {
        // parse the fragment
        final ParseResult<IStrategoTerm> parseRes;
        try {
            parseRes = fragmentParser.parse(test.getFragment(), languageUnderTest);
        } catch(ParseException e) {
            // TODO: is this ok? or should we fail the test and gracefully return a message?
            throw new RuntimeException(e);
        }

        // analyze the fragment if any expectation requires analysis
        AnalysisResult<IStrategoTerm, IStrategoTerm> analysisRes = null;
        for(ExpectationPair expectationPair : test.getExpectations()) {
            ITestExpectation evaluator = expectationPair.evaluator;
            if(evaluator == null) {
                // The TestExtractor may return tests with missing ITestExpectations
                // if this happens, the ExtractorResult should have signalled that it failed
                // we get here if the user blatantly ignores that,
                // So we will assume the user simply doesn't care about this expectation
                continue;
            }
            IStrategoTerm expectation = expectationPair.expectation;
            if(evaluator.getPhase(expectation).ordinal() > TestPhase.PARSING.ordinal()) {
                // TODO: what if the resource is null?
                try(ITemporaryContext ctx =
                    contextService.getTemporary(test.getResource(), project, languageUnderTest)) {
                    analysisRes = analysisService.analyze(Iterables2.singleton(parseRes), ctx);
                } catch(ContextException | AnalysisException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }

        // evaluate the test expectations
        // TODO: handle the 'no expectation means parsing must succeed' thing
        boolean success = true;
        List<ITestExpectationOutput> expectationOutputs = new ArrayList<>();
        for(ExpectationPair expectationPair : test.getExpectations()) {
            ITestExpectation evaluator = expectationPair.evaluator;
            if(evaluator == null) {
                // The TestExtractor may return tests with missing ITestExpectations
                // if this happens, the ExtractorResult should have signalled that it failed
                // we get here if the user blatantly ignores that,
                // So we will assume the user simply doesn't care about this expectation
                continue;
            }
            IStrategoTerm expectation = expectationPair.expectation;
            TestExpectationInput input =
                new TestExpectationInput(test, expectation, languageUnderTest, parseRes, analysisRes);
            ITestExpectationOutput output = evaluator.evaluate(input);
            if(!output.isSuccessful()) {
                success = false;
            }
            expectationOutputs.add(output);
        }

        return new TestResult(success, Iterables2.<IMessage>empty(), expectationOutputs);
    }

}
