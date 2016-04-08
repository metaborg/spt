package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase.ExpectationPair;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class TestCaseRunner implements ITestCaseRunner {

    private static final ILogger logger = LoggerUtils.logger(TestCaseRunner.class);

    private final ISpoofaxAnalysisService analysisService;
    private final IContextService contextService;
    private final IFragmentParser fragmentParser;

    @Inject public TestCaseRunner(Set<ITestExpectation> expectations, ISpoofaxAnalysisService analysisService,
        IContextService contextService, IFragmentParser fragmentParser) {
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.fragmentParser = fragmentParser;
    }

    @Override public ITestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
        @Nullable ILanguageImpl dialectUnderTest) {
        logger.debug("About to run test case '{}' with language {}", test.getDescription(), languageUnderTest.id());

        // parse the fragment
        final ISpoofaxParseUnit parseRes;
        try {
            parseRes = fragmentParser.parse(test.getFragment(), languageUnderTest, dialectUnderTest);
        } catch(ParseException e) {
            // TODO: is this ok? or should we fail the test and gracefully return a message?
            throw new RuntimeException(e);
        }

        // analyze the fragment if any expectation requires analysis
        ISpoofaxAnalyzeUnit analysisRes = null;
        ITemporaryContext context = null;
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
                try {
                    context = contextService.getTemporary(test.getResource(), project, languageUnderTest);
                    analysisRes = analysisService.analyze(parseRes, context).result();
                } catch(ContextException | AnalysisException e) {
                    if(context != null) {
                        context.close();
                    }
                    // TODO: is this ok? or should we fail the test and gracefully return a message?
                    throw new RuntimeException(e);
                }
                break;
            }
        }

        // evaluate the test expectations
        boolean success = true;
        List<ITestExpectationOutput> expectationOutputs = new ArrayList<>();
        if(test.getExpectations().isEmpty()) {
            // handle the 'no expectation means parsing must succeed' thing
            success = parseRes.success();
        } else {
            for(ExpectationPair expectationPair : test.getExpectations()) {
                ITestExpectation evaluator = expectationPair.evaluator;
                if(evaluator == null) {
                    // The TestExtractor may return tests with missing ITestExpectations
                    // if this happens, the ExtractorResult should have signaled that it failed
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
        }

        // close the analysis context for this test run
        if(context != null) {
            context.close();
        }

        return new TestResult(success, Iterables2.<IMessage>empty(), expectationOutputs);
    }

}
