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
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class WhitespacingTestCaseRunner implements ITestCaseRunner {

    private final Set<ITestExpectation> expectationServices;
    private final ISyntaxService<IStrategoTerm> parseService;
    private final IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService;
    private final IContextService contextService;

    @Inject public WhitespacingTestCaseRunner(Set<ITestExpectation> expectations,
        ISyntaxService<IStrategoTerm> parseService, IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService,
        IContextService contextService) {
        this.expectationServices = expectations;
        this.parseService = parseService;
        this.analysisService = analysisService;
        this.contextService = contextService;
    }

    public ITestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest) {

        // lookup the ITestExpectations that can handle our test expectations
        final List<ITestExpectation> expectationHandlers = new ArrayList<>();
        for(IStrategoTerm expectationTerm : test.getExpectations()) {
            boolean found = false;
            for(ITestExpectation expectation : expectationServices) {
                if(expectation.canEvaluate(expectationTerm)) {
                    expectationHandlers.add(expectation);
                    found = true;
                    break;
                }
            }
            if(!found) {
                ISourceRegion region = SPTUtil.getRegion(expectationTerm);
                IMessage m =
                    MessageBuilder.create().asError().asAnalysis().withSource(test.getResource()).withRegion(region)
                        .withMessage(
                            "Unable to evaluate this test expectation. No ITestExpectation found that can handle this.")
                    .build();
                return new TestResult(false, Iterables2.singleton(m), Iterables2.<ITestExpectationOutput>empty());
            }
        }

        // parse the fragment
        final ParseResult<IStrategoTerm> parseRes;
        try {

            parseRes = parseService.parse(SPTUtil.getFragmentTextUsingWhitespace(allText, fragmentTerm),
                test.getResource(), languageUnderTest, null);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }

        // analyze the fragment if any expectation requires analysis
        AnalysisResult<IStrategoTerm, IStrategoTerm> analysisRes = null;
        final List<IStrategoTerm> expectations = test.getExpectations();
        for(int i = 0; i < expectations.size(); i++) {
            ITestExpectation handler = expectationHandlers.get(i);
            IStrategoTerm expectation = expectations.get(i);
            if(handler.getPhase(expectation).ordinal() > TestPhase.PARSING.ordinal()) {
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
        for(int i = 0; i < expectations.size(); i++) {
            ITestExpectation handler = expectationHandlers.get(i);
            IStrategoTerm expectation = expectations.get(i);
            TestExpectationInput input =
                new TestExpectationInput(test, expectation, languageUnderTest, parseRes, analysisRes);
            ITestExpectationOutput output = handler.evaluate(input);
            if(!output.isSuccessful()) {
                success = false;
            }
            expectationOutputs.add(output);
        }

        return new TestResult(success, Iterables2.<IMessage>empty(), expectationOutputs);
    }

}
