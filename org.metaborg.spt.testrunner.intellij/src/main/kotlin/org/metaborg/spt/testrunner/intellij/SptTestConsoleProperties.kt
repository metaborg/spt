package org.metaborg.spt.testrunner.intellij

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator

/**
 * Describes how the test console should work, for example, how test locations are resolved.
 *
 * @see <a href="https://intellij-support.jetbrains.com/hc/en-us/community/posts/206103879-Graphical-integration-of-running-tests-in-plugin">
 *     Graphical integration of running tests in plugin</a>
 */
class SptTestConsoleProperties(config: RunConfiguration,
                               executor: Executor)
    : SMTRunnerConsoleProperties(config, FRAMEWORK_NAME, executor) {

    companion object {
        const val FRAMEWORK_NAME = "SptTestRunner"
    }
}