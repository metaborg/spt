package org.metaborg.spt.core.run;

import java.lang.reflect.ParameterizedType;

import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Types;

public class SpoofaxExpectationEvaluatorService implements ISpoofaxExpectationEvaluatorService {

    private static final ILogger logger = LoggerUtils.logger(SpoofaxExpectationEvaluatorService.class);

    private final Injector injector;

    @jakarta.inject.Inject public SpoofaxExpectationEvaluatorService(Injector injector) {
        this.injector = injector;
    }

    @SuppressWarnings("unchecked") @Override public <E extends ITestExpectation> ISpoofaxExpectationEvaluator<E>
        lookup(E expectation) {
        ParameterizedType evaType =
            Types.newParameterizedType(ISpoofaxExpectationEvaluator.class, expectation.getClass());
        try {
            return (ISpoofaxExpectationEvaluator<E>) injector.getInstance(Key.get(evaType));
        } catch(ConfigurationException e) {
            logger.info("Unable to find an evaluator for expectation of type {}", e, expectation.getClass());
            return null;
        }
    }

}
