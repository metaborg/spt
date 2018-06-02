package org.metaborg.spt.core.extract.expectations;

import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.NoExpectationError;
import org.metaborg.mbt.core.model.expectations.NotExpectation;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class NotExpectationProvider implements ISpoofaxTestExpectationProvider {
    private static final ILogger logger = LoggerUtils.logger(NotExpectationProvider.class);

    private static final String CONS = "Not";

    private final Set<ISpoofaxTestExpectationProvider> expectationProviders;


    @Inject public NotExpectationProvider(Set<ISpoofaxTestExpectationProvider> expectationProviders) {
        this.expectationProviders = expectationProviders;
    }


    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final String cons = SPTUtil.consName(expectationTerm);
        return CONS.equals(cons) && createSubExpectation(inputFragment, expectationTerm) != null;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final ITestExpectation subExpectation = createSubExpectation(inputFragment, expectationTerm);
        if(subExpectation == null) {
            logger.warn("Unable to find a provider for sub expectation {}", expectationTerm);
            return new NoExpectationError(inputFragment.getRegion());
        }
        return new NotExpectation(inputFragment.getRegion(), subExpectation);
    }


    private @Nullable ITestExpectation createSubExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final IStrategoTerm subExpectationTerm = expectationTerm.getSubterm(0);
        for(ISpoofaxTestExpectationProvider provider : expectationProviders) {
            if(provider.canEvaluate(inputFragment, subExpectationTerm)) {
                final ITestExpectation expectation = provider.createExpectation(inputFragment, subExpectationTerm);
                return expectation;
            }
        }
        return null;
    }
}
