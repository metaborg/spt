package org.metaborg.spt.testrunner.intellij

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

/**
 * A factory for SPT test run configurations.
 *
 * The factory is responsible for creating a template configuration with
 * sensible defaults, which is then modified by the user. A single run configuration type
 * can have more than one factory; for example a web server could have
 * a local and a remote run configuration factory.
 *
 * @see <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html">
 *     IntelliJ Platform SDK DevGuide: Run Configuration Management</a>
 */
class SptTestConfigurationFactory(ctype: ConfigurationType) : ConfigurationFactory(ctype) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return SptTestConfiguration("", project, this)
    }
}