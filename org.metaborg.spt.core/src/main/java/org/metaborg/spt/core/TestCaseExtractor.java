package org.metaborg.spt.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Inject;

public class TestCaseExtractor implements ITestCaseExtractor {

    private final ISyntaxService<IStrategoTerm> parseService;

    @Inject TestCaseExtractor(ISyntaxService<IStrategoTerm> parseService) {
        this.parseService = parseService;
    }

    @Override public Iterable<ITestCase> extract(ILanguageImpl spt, IProject project, final FileObject testSuite) {
        InputStream in;
        final IStrategoTerm ast;
        try {
            in = testSuite.getContent().getInputStream();
            String text = IOUtils.toString(in);
            in.close();
            ast = parseService.parse(text, testSuite, spt, null).result;
        } catch(IOException | ParseException e) {
            throw new IllegalArgumentException(e);
        }

        final List<ITestCase> tests = new ArrayList<>();
        final ITestCaseBuilder builder = new TestCaseBuilder();
        new TermVisitor() {

            @Override public void preVisit(IStrategoTerm term) {
                if(Term.isTermAppl(term)) {
                    String cons = ((IStrategoAppl) term).getConstructor().getName();
                    if("Test2".equals(cons) || "Test3".equals(cons) || "Test4".equals(cons)) {
                        tests.add(builder.withTest(term, testSuite).build());
                    }
                }
            }
        }.visit(ast);

        return tests;
    }
}
