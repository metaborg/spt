package org.metaborg.spt.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

public class TestCaseExtractor implements ITestCaseExtractor {

    private final ISyntaxService<IStrategoTerm> parseService;
    private final IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService;
    private final IContextService contextService;
    private final ITestCaseBuilder testBuilder;

    @Inject public TestCaseExtractor(ISyntaxService<IStrategoTerm> parseService,
        IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService, IContextService contextService,
        ITestCaseBuilder builder) {
        this.parseService = parseService;
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.testBuilder = builder;
    }

    @Override public ITestCaseExtractionResult extract(ILanguageImpl spt, IProject project,
        final FileObject testSuite) {
        InputStream in;
        final ParseResult<IStrategoTerm> parseResult;
        final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult;
        try {
            in = testSuite.getContent().getInputStream();
            String text = IOUtils.toString(in);
            in.close();
            parseResult = parseService.parse(text, testSuite, spt, null);
            if(parseResult.result == null) {
                // parse failed and couldn't recover
                return new TestCaseExtractionResult(parseResult, null, Iterables2.<ITestCase>empty());
            }
            // even if parsing fails we can still analyze
            // the result will just be empty
            IContext ctx = contextService.get(testSuite, project, spt);
            analysisResult = analysisService.analyze(Iterables2.singleton(parseResult), ctx);
        } catch(IOException | ParseException | ContextException | AnalysisException e) {
            throw new IllegalArgumentException("Failed to extract tests due to a previous exception.", e);
        }

        // Retrieve the AST from the analysis result
        Iterable<AnalysisFileResult<IStrategoTerm, IStrategoTerm>> analysisFileResults = analysisResult.fileResults;
        if(Iterables.isEmpty(analysisFileResults)) {
            // analysis apparently failed? We have no analyzed ast.
            return new TestCaseExtractionResult(parseResult, analysisResult, Iterables2.<ITestCase>empty());
        }
        final IStrategoTerm ast = analysisResult.fileResults.iterator().next().value();
        if(Iterables.isEmpty(analysisFileResults)) {
            // analysis apparently failed? We have no analyzed ast.
            return new TestCaseExtractionResult(parseResult, analysisResult, Iterables2.<ITestCase>empty());
        }

        final List<ITestCase> tests = new ArrayList<>();
        new TermVisitor() {

            @Override public void preVisit(IStrategoTerm term) {
                if(Term.isTermAppl(term)) {
                    if(SPTUtil.TEST_CONS.equals(SPTUtil.consName(term))) {
                        tests.add(testBuilder.withTest(term, testSuite).build());
                    }
                }
            }
        }.visit(ast);

        return new TestCaseExtractionResult(parseResult, analysisResult, tests);
    }
}
