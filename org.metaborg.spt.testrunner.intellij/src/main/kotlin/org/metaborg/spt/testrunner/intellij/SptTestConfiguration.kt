package org.metaborg.spt.testrunner.intellij

import com.intellij.execution.*
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.JDOMExternalizer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.jdom.Element
import java.util.*

/**
 * A run configuration for SPT tests.
 *
 * Any run configuration is an instance of the [RunConfiguration] interface,
 * and this instance holds all the configuration for that particular run configuration
 * including any changes made by the user. Depending on the requirements, you may want to
 * derive your run configuration from one of the base run configuration classes
 * provided by IntelliJ. See the documentation for more details.
 *
 * Run configuration persistence must be implemented in the [readExternal]
 * and [writeExternal] methods, to allow a run configuration to be saved
 * and loaded across sessions.
 *
 * @see <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html">
 *     IntelliJ Platform SDK DevGuide: Run Configuration Management</a>
 * @see <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_execution.html">
 *     IntelliJ Platform SDK DevGuide: Execution</a>
 */
class SptTestConfiguration(
        name: String,
        project: Project,
        factory: ConfigurationFactory)
    : ModuleBasedConfiguration<RunConfigurationModule>(name, RunConfigurationModule(project), factory),
        CommonJavaRunConfigurationParameters {

    private var envs: MutableMap<String, String> = LinkedHashMap()
    override fun getEnvs(): MutableMap<String, String> = this.envs
    override fun setEnvs(envs: MutableMap<String, String>) { this.envs = envs }

    private var alternativeJrePath: String? = null
    override fun getAlternativeJrePath(): String? = if (this.alternativeJrePathEnabled) this.alternativeJrePath else null
    override fun setAlternativeJrePath(path: String?) { this.alternativeJrePath = path }

    private var passParentEnvs: Boolean = true
    override fun isPassParentEnvs(): Boolean = this.passParentEnvs
    override fun setPassParentEnvs(passParentEnvs: Boolean) { this.passParentEnvs = passParentEnvs }

    private var programParameters: String? = null
    override fun getProgramParameters(): String? = this.programParameters
    override fun setProgramParameters(value: String?) { this.programParameters = value }

    private var vmParameters: String? = null
    override fun getVMParameters(): String? = this.vmParameters
    override fun setVMParameters(value: String?) { this.vmParameters = value }

    private var alternativeJrePathEnabled: Boolean = false
    override fun isAlternativeJrePathEnabled(): Boolean = this.alternativeJrePathEnabled
    override fun setAlternativeJrePathEnabled(enabled: Boolean) { this.alternativeJrePathEnabled = enabled }

    private var workingDirectory: String? = null
    override fun getWorkingDirectory(): String? = this.workingDirectory
    override fun setWorkingDirectory(value: String?) { this.workingDirectory = value }

    private var testPath: String? = null
    fun getTestPath(): String? = this.testPath
    fun setTestPath(value: String?) { this.testPath = value }

    override fun getRunClass(): String? = null
    override fun getPackage(): String? = null

    override fun getValidModules(): MutableCollection<Module> {
        // TODO: Return only modules that are valid Spoofax languages?
        return ModuleManager.getInstance(this.project).modules.toMutableList()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val module = configurationModule.module!!
        return object: SptCommandLineState(environment, this, this.project, module) {
            override fun createJavaParameters(): JavaParameters? {
                val params = super.createJavaParameters() ?: return null
                params.setDefaultCharset(this@SptTestConfiguration.project)
                params.jdk = getJdk(module)
                return params
            }
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return SptTestConfigurationEditor(project)
    }

    override fun checkConfiguration() {
        val module = this.configurationModule.module
        if (module == null)
            throw RuntimeConfigurationException("No language module specified")
        if (module.isDisposed)
            throw RuntimeConfigurationException("Language module is disposed")
        JavaParametersUtil.checkAlternativeJRE(this)

        val testPath = getTestPath()
        if (testPath == null || testPath.isNullOrBlank())
            throw RuntimeConfigurationException("No test folder specified")
        val testFolder = LocalFileSystem.getInstance().findFileByPath(testPath)
        if (testFolder == null || !testFolder.exists())
            throw RuntimeConfigurationException("The test folder does not exist")
        // TODO: Check that the folder actually contains .spt files?

        super.checkConfiguration()
    }

    override fun readExternal(element: Element) {
        PathMacroManager.getInstance(project).expandPaths(element)
        super.readExternal(element)
        readModule(element)
        this.testPath = JDOMExternalizer.readString(element, "testPath")
        this.vmParameters = JDOMExternalizer.readString(element, "vmparams")
        this.programParameters = JDOMExternalizer.readString(element, "params")
        this.workingDirectory = pathNonLocalOrNull(JDOMExternalizer.readString(element, "workDir"))
        this.envs = readMap(element, this.envs, "env")
        this.passParentEnvs = JDOMExternalizer.readBoolean(element, "passParentEnv")
        this.alternativeJrePathEnabled = JDOMExternalizer.readBoolean(element, "alternativeJrePathEnabled")
        this.alternativeJrePath = JDOMExternalizer.readString(element, "alternativeJrePath")
        JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        writeModule(element)

        JDOMExternalizer.write(element, "testPath", this.testPath)
        JDOMExternalizer.write(element, "vmparams", this.vmParameters)
        JDOMExternalizer.write(element, "params", this.programParameters)
        JDOMExternalizer.write(element, "workDir", this.workingDirectory)
        JDOMExternalizer.writeMap(element, this.envs, null, "env")
        JDOMExternalizer.write(element, "passParentEnv", this.passParentEnvs)

        if (this.alternativeJrePathEnabled) {
            JDOMExternalizer.write(element, "alternativeJrePathEnabled", true)
            if (StringUtil.isNotEmpty(this.alternativeJrePath))
                JDOMExternalizer.write(element, "alternativeJrePath", this.getAlternativeJrePath())
        }

        JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element)

        PathMacroManager.getInstance(project).collapsePathsRecursively(element)
    }

    /**
     * Returns the given path if it is not "."; otherwise, null.
     *
     * @param path The path to check.
     */
    fun pathNonLocalOrNull(path: String?): String? = if (path == ".") null else path

    /**
     * Reads a map.
     *
     * @param element The element to read from.
     * @param map The map to fill with the read values; or null.
     * @param entryName The entry name.
     * @return The given map, or a new one.
     */
    fun readMap(element: Element, map: MutableMap<String, String>?, entryName: String): MutableMap<String, String> {
        val returnedMap = map ?: LinkedHashMap()
        returnedMap.clear()
        JDOMExternalizer.readMap(element, returnedMap, null, entryName)
        return returnedMap
    }

    /**
     * Gets the JDK for the module if not null; otherwise, the project.
     *
     * @param module The module; or null.
     * @return The JDK.
     */
    fun getJdk(module: Module?): Sdk {
        val jrePath = this.getAlternativeJrePath()

        if (module == null)
            return JavaParametersUtil.createProjectJdk(this.project, jrePath)
        else
            return JavaParametersUtil.createModuleJdk(module, false, jrePath)
    }
}