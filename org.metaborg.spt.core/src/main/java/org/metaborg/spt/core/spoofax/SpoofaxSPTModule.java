package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IExpectationEvaluator;
import org.metaborg.spt.core.IExpectationEvaluatorService;
import org.metaborg.spt.core.IFragmentBuilder;
import org.metaborg.spt.core.IFragmentParser;
import org.metaborg.spt.core.ITestCaseBuilder;
import org.metaborg.spt.core.ITestCaseExtractor;
import org.metaborg.spt.core.ITestCaseRunner;
import org.metaborg.spt.core.ITestExpectationProvider;
import org.metaborg.spt.core.SPTModule;
import org.metaborg.spt.core.expectations.AnalysisMessageExpectation;
import org.metaborg.spt.core.expectations.ParseExpectation;
import org.metaborg.spt.core.expectations.ResolveExpectation;
import org.metaborg.spt.core.expectations.RunStrategoExpectation;
import org.metaborg.spt.core.expectations.TransformExpectation;
import org.metaborg.spt.core.spoofax.expectations.AnalyzeExpectationEvaluator;
import org.metaborg.spt.core.spoofax.expectations.AnalyzeExpectationProvider;
import org.metaborg.spt.core.spoofax.expectations.FragmentUtil;
import org.metaborg.spt.core.spoofax.expectations.ParseExpectationEvaluator;
import org.metaborg.spt.core.spoofax.expectations.ParseExpectationProvider;
import org.metaborg.spt.core.spoofax.expectations.ResolveExpectationEvaluator;
import org.metaborg.spt.core.spoofax.expectations.ResolveExpectationProvider;
import org.metaborg.spt.core.spoofax.expectations.RunStrategoExpectationEvaluator;
import org.metaborg.spt.core.spoofax.expectations.RunStrategoExpectationProvider;
import org.metaborg.spt.core.spoofax.expectations.TransformExpectationEvaluator;
import org.metaborg.spt.core.spoofax.expectations.TransformExpectationProvider;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class SpoofaxSPTModule extends SPTModule {

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
    }

    @Override protected void configureExpectationEvaluators() {
        super.configureExpectationProviders();
        // evaluator service
        bind(SpoofaxExpectationEvaluatorService.class);
        bind(new TypeLiteral<IExpectationEvaluatorService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>>() {})
            .to(SpoofaxExpectationEvaluatorService.class);
        bind(ISpoofaxExpectationEvaluatorService.class).to(SpoofaxExpectationEvaluatorService.class);

        // parsing
        bind(new TypeLiteral<IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ParseExpectation>>() {})
            .to(ParseExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<ParseExpectation>>() {}).to(ParseExpectationEvaluator.class);
        // analysis - messages
        bind(
            new TypeLiteral<IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, AnalysisMessageExpectation>>() {})
                .to(AnalyzeExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<AnalysisMessageExpectation>>() {})
            .to(AnalyzeExpectationEvaluator.class);
        // analysis - resolving
        bind(new TypeLiteral<IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ResolveExpectation>>() {})
            .to(ResolveExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<ResolveExpectation>>() {})
            .to(ResolveExpectationEvaluator.class);
        // analysis - running stratego
        bind(
            new TypeLiteral<IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, RunStrategoExpectation>>() {})
                .to(RunStrategoExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<RunStrategoExpectation>>() {})
            .to(RunStrategoExpectationEvaluator.class);
        // transformation
        bind(new TypeLiteral<IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, TransformExpectation>>() {})
            .to(TransformExpectationEvaluator.class);
        bind(new TypeLiteral<ISpoofaxExpectationEvaluator<TransformExpectation>>() {})
            .to(TransformExpectationEvaluator.class);
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
        bind(ITestCaseRunner.class).to(SpoofaxTestCaseRunner.class);
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
        bind(SpoofaxWhitespaceFragmentParser.class).in(Singleton.class);
        bind(ISpoofaxFragmentParser.class).to(SpoofaxWhitespaceFragmentParser.class);
        bind(new TypeLiteral<IFragmentParser<?, ?>>() {}).to(SpoofaxWhitespaceFragmentParser.class);
        bind(new TypeLiteral<IFragmentParser<ISpoofaxInputUnit, ISpoofaxParseUnit>>() {})
            .to(SpoofaxWhitespaceFragmentParser.class);
    }

    @Override public void configureUtil() {
        bind(FragmentUtil.class);
    }

}
