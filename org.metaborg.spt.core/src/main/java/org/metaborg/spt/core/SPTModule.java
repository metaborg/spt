package org.metaborg.spt.core;

import org.metaborg.mbt.core.MBTModule;
import org.metaborg.mbt.core.extract.IFragmentBuilder;
import org.metaborg.mbt.core.extract.ITestCaseBuilder;
import org.metaborg.mbt.core.extract.ITestCaseExtractor;
import org.metaborg.mbt.core.extract.ITestExpectationProvider;
import org.metaborg.mbt.core.model.expectations.AnalysisMessageExpectation;
import org.metaborg.mbt.core.model.expectations.HasOriginExpectation;
import org.metaborg.mbt.core.model.expectations.ParseExpectation;
import org.metaborg.mbt.core.model.expectations.ResolveExpectation;
import org.metaborg.mbt.core.model.expectations.TransformExpectation;
import org.metaborg.mbt.core.run.IExpectationEvaluatorService;
import org.metaborg.mbt.core.run.IFragmentParser;
import org.metaborg.mbt.core.run.ITestCaseRunner;
import org.metaborg.mbt.core.run.ITestExpectationEvaluator;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.expectations.ParseToAtermExpectation;
import org.metaborg.spt.core.expectations.RunStrategoExpectation;
import org.metaborg.spt.core.expectations.RunStrategoToAtermExpectation;
import org.metaborg.spt.core.expectations.TransformToAtermExpectation;
import org.metaborg.spt.core.extract.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractor;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.extract.SpoofaxTestCaseBuilder;
import org.metaborg.spt.core.extract.SpoofaxTestCaseExtractor;
import org.metaborg.spt.core.extract.SpoofaxTracingFragmentBuilder;
import org.metaborg.spt.core.extract.expectations.AnalyzeExpectationProvider;
import org.metaborg.spt.core.extract.expectations.HasOriginExpectationProvider;
import org.metaborg.spt.core.extract.expectations.ParseExpectationProvider;
import org.metaborg.spt.core.extract.expectations.ParseToAtermExpectationProvider;
import org.metaborg.spt.core.extract.expectations.ResolveExpectationProvider;
import org.metaborg.spt.core.extract.expectations.RunStrategoExpectationProvider;
import org.metaborg.spt.core.extract.expectations.RunStrategoToAtermExpectationProvider;
import org.metaborg.spt.core.extract.expectations.TransformExpectationProvider;
import org.metaborg.spt.core.extract.expectations.TransformToAtermExpectationProvider;
import org.metaborg.spt.core.run.FragmentUtil;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluatorService;
import org.metaborg.spt.core.run.ISpoofaxFragmentParser;
import org.metaborg.spt.core.run.ISpoofaxTestCaseRunner;
import org.metaborg.spt.core.run.SpoofaxExpectationEvaluatorService;
import org.metaborg.spt.core.run.SpoofaxOriginFragmentParser;
import org.metaborg.spt.core.run.SpoofaxTestCaseRunner;
import org.metaborg.spt.core.run.expectations.AnalyzeExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.HasOriginExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.ParseExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.ParseToAtermExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.ResolveExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.RunStrategoExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.RunStrategoToAtermExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.TransformExpectationEvaluator;
import org.metaborg.spt.core.run.expectations.TransformToAtermExpectationEvaluator;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class SPTModule extends MBTModule {
    @Override protected void configure() {
        super.configure();
        
        bind(SPTRunner.class).in(Singleton.class);
    }

    @Override protected void configureExpectationProviders() {
        super.configureExpectationProviders();
        Multibinder<ITestExpectationProvider<IStrategoTerm>> expectationBinder =
            Multibinder.newSetBinder(binder(), new TypeLiteral<ITestExpectationProvider<IStrategoTerm>>() {});
        expectationBinder.addBinding().to(ParseExpectationProvider.class);
        expectationBinder.addBinding().to(AnalyzeExpectationProvider.class);
        expectationBinder.addBinding().to(ResolveExpectationProvider.class);
        expectationBinder.addBinding().to(RunStrategoExpectationProvider.class);
        expectationBinder.addBinding().to(TransformExpectationProvider.class);
        Multibinder<ISpoofaxTestExpectationProvider> expectationBinder2 =
            Multibinder.newSetBinder(binder(), ISpoofaxTestExpectationProvider.class);
        expectationBinder2.addBinding().to(ParseExpectationProvider.class);
        expectationBinder2.addBinding().to(AnalyzeExpectationProvider.class);
        expectationBinder2.addBinding().to(ResolveExpectationProvider.class);
        expectationBinder2.addBinding().to(RunStrategoExpectationProvider.class);
        expectationBinder2.addBinding().to(TransformExpectationProvider.class);
        expectationBinder2.addBinding().to(HasOriginExpectationProvider.class);
        // Spoofax specific binders
        expectationBinder2.addBinding().to(ParseToAtermExpectationProvider.class);
        expectationBinder2.addBinding().to(RunStrategoToAtermExpectationProvider.class);
        expectationBinder2.addBinding().to(TransformToAtermExpectationProvider.class);
    }

    @Override protected void configureExpectationEvaluators() {
        super.configureExpectationProviders();
        // evaluator service
        bind(SpoofaxExpectationEvaluatorService.class);
        bind(new TypeLiteral<IExpectationEvaluatorService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>>() {})
            .to(SpoofaxExpectationEvaluatorService.class);
        bind(ISpoofaxExpectationEvaluatorService.class).to(SpoofaxExpectationEvaluatorService.class);

        // parsing
        bind(new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ParseExpectation>>() {})
            .to(ParseExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<ParseExpectation>>() {}).to(ParseExpectationEvaluator.class);
        // analysis - messages
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, AnalysisMessageExpectation>>() {})
                .to(AnalyzeExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<AnalysisMessageExpectation>>() {})
            .to(AnalyzeExpectationEvaluator.class);
        // analysis - resolving
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ResolveExpectation>>() {})
                .to(ResolveExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<ResolveExpectation>>() {})
            .to(ResolveExpectationEvaluator.class);
        // analysis - running stratego
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, RunStrategoExpectation>>() {})
                .to(RunStrategoExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<RunStrategoExpectation>>() {})
            .to(RunStrategoExpectationEvaluator.class);
        // transformation
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, TransformExpectation>>() {})
                .to(TransformExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<TransformExpectation>>() {})
            .to(TransformExpectationEvaluator.class);
        // origin expectation
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, HasOriginExpectation>>() {})
                .to(HasOriginExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<HasOriginExpectation>>() {})
            .to(HasOriginExpectationEvaluator.class);

        // To ATERM stuff
        // parse to aterm (very Spoofax specific)
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ParseToAtermExpectation>>() {})
                .to(ParseToAtermExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<ParseToAtermExpectation>>() {})
            .to(ParseToAtermExpectationEvaluator.class);
        // run to aterm (very Spoofax specific)
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, RunStrategoToAtermExpectation>>() {})
                .to(RunStrategoToAtermExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<RunStrategoToAtermExpectation>>() {})
            .to(RunStrategoToAtermExpectationEvaluator.class);
        // transform to aterm (very Spoofax specific)
        bind(
            new TypeLiteral<ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, TransformToAtermExpectation>>() {})
                .to(TransformToAtermExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<TransformToAtermExpectation>>() {})
            .to(TransformToAtermExpectationEvaluator.class);
    }

    @Override protected void configureExtractor() {
        bind(SpoofaxTestCaseExtractor.class).in(Singleton.class);
        bind(ISpoofaxTestCaseExtractor.class).to(SpoofaxTestCaseExtractor.class);
        bind(new TypeLiteral<ITestCaseExtractor<ISpoofaxInputUnit, ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>>() {})
            .to(SpoofaxTestCaseExtractor.class);
        bind(new TypeLiteral<ITestCaseExtractor<?, ?, ?>>() {}).to(SpoofaxTestCaseExtractor.class);
    }

    @Override protected void configureRunner() {
        bind(SpoofaxTestCaseRunner.class).in(Singleton.class);
        bind(new TypeLiteral<ITestCaseRunner<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>>() {})
            .to(SpoofaxTestCaseRunner.class);
        bind(ISpoofaxTestCaseRunner.class).to(SpoofaxTestCaseRunner.class);
    }

    @Override protected void configureBuilders() {
        bind(ISpoofaxTestCaseBuilder.class).to(SpoofaxTestCaseBuilder.class);
        bind(new TypeLiteral<ITestCaseBuilder<IStrategoTerm, IStrategoTerm>>() {}).to(SpoofaxTestCaseBuilder.class);
        bind(new TypeLiteral<ITestCaseBuilder<?, ?>>() {}).to(SpoofaxTestCaseBuilder.class);

        bind(ISpoofaxFragmentBuilder.class).to(SpoofaxTracingFragmentBuilder.class);
        bind(new TypeLiteral<IFragmentBuilder<IStrategoTerm, IStrategoTerm>>() {})
            .to(SpoofaxTracingFragmentBuilder.class);
        bind(new TypeLiteral<IFragmentBuilder<?, ?>>() {}).to(SpoofaxTracingFragmentBuilder.class);
    }

    @Override protected void configureFragmentParser() {
        // for now, we keep using the whitespace hack
        // bind(SpoofaxWhitespaceFragmentParser.class).in(Singleton.class);
        // bind(ISpoofaxFragmentParser.class).to(SpoofaxWhitespaceFragmentParser.class);
        // bind(new TypeLiteral<IFragmentParser<?>>() {}).to(SpoofaxWhitespaceFragmentParser.class);
        // bind(new TypeLiteral<IFragmentParser<ISpoofaxParseUnit>>() {}).to(SpoofaxWhitespaceFragmentParser.class);

        // this is a test for the new fragment parser
        bind(SpoofaxOriginFragmentParser.class).in(Singleton.class);
        bind(ISpoofaxFragmentParser.class).to(SpoofaxOriginFragmentParser.class);
        bind(new TypeLiteral<IFragmentParser<?>>() {}).to(SpoofaxOriginFragmentParser.class);
        bind(new TypeLiteral<IFragmentParser<ISpoofaxParseUnit>>() {}).to(SpoofaxOriginFragmentParser.class);
    }

    @Override public void configureUtil() {
        bind(FragmentUtil.class);
    }

}
