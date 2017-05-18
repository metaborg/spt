package org.metaborg.spt.testrunner.intellij

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.metaborg.core.testing.TeamCityTestReporterService
import org.metaborg.spt.testrunner.SptPlugin
import java.io.File

/**
 * Sets up and executes the SPT command in a separate process.
 *
 * This is a special command-line state as it can handle test results. This was achieved by overriding
 * the [createConsole] method to return a test console. The test console can handle and display the test
 * results as long as the test runner process returns TeamCity service messages on stdout.
 *
 * @param environment The execution environment.
 * @property configuration The test configuration.
 * @property project The project.
 * @property module The module; or null.
 *
 * @see <a href="https://intellij-support.jetbrains.com/hc/en-us/community/posts/206103879-Graphical-integration-of-running-tests-in-plugin">
 *     Graphical integration of running tests in plugin</a>
 * @see <a href="https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity">
 *     Build Script Interaction with TeamCity</a>
 */
open class SptCommandLineState(environment: ExecutionEnvironment,
                          val configuration: SptTestConfiguration,
                          private val project: Project,
                          private val module: Module)
    : JavaCommandLineState(environment) {

    private val SPT_CMD_MAIN  = "org.metaborg.spt.cmd.Main"
    private val SPT_CMD_PATH  = File(SptPlugin.libPath, "org.metaborg.spt.cmd-${SptPlugin.version}.jar").absolutePath
    private val SPT_LANG_PATH = File(SptPlugin.libPath, "org.metaborg.meta.lang.spt-${SptPlugin.version}.spoofax-language").absolutePath

    override fun createJavaParameters(): JavaParameters? {
        val languageUnderTest = LanguageUtils.getLanguageRoot(this.module) ?: return null
        val params = JavaParameters().apply {
            // Set up the class path
            classPath.add(SPT_CMD_PATH)
            isUseClasspathJar = true
            // The SPT command-line utility main class
            mainClass = SPT_CMD_MAIN
            // The language under test
            programParametersList.add("--lut", languageUnderTest.path)
            // The SPT language artifact/directory
            programParametersList.add("--spt", SPT_LANG_PATH)
            // The directory with the SPT tests
            programParametersList.add("--tests", configuration.getTestPath())
            // The test reporter to use (so IntelliJ can display the results)
            programParametersList.add("--reporter", TeamCityTestReporterService::class.java.name)
        }

        return params
    }

    /**
     * Creates the console that can show the test runner's output as test results.
     */
    override fun createConsole(executor: Executor): ConsoleView? {
        val consoleProperties = createConsoleProperties(executor)
        return SMTestRunnerConnectionUtil.createConsole(SptTestConsoleProperties.FRAMEWORK_NAME, consoleProperties)
    }

    private fun createConsoleProperties(executor: Executor): SptTestConsoleProperties {
        return SptTestConsoleProperties(this.configuration, executor)
    }

}