package org.metaborg.spt.testrunner.intellij

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import org.metaborg.spt.testrunner.SptIcons
import javax.swing.Icon

/**
 * A run configuration type for SPT tests.
 *
 * Any run configuration in IntelliJ starts with a run configuration _type_.
 * The type describes the type of run configurations through its name, description,
 * and icon. The identifier of a run configuration type should be the same as the
 * name of the run configuration class (but this is not required).
 *
 * @see <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html">
 *     IntelliJ Platform SDK DevGuide: Run Configuration Management</a>
 */
class SptTestConfigurationType : ConfigurationType {

    override fun getDisplayName(): String = "Spoofax SPT"

    override fun getIcon(): Icon = SptIcons.defaultIcon

    override fun getConfigurationTypeDescription(): String =
            "Spoofax SPT testing framework run configurations"

    override fun getId(): String = SptTestConfiguration::class.java.simpleName

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(SptTestConfigurationFactory(this))
}