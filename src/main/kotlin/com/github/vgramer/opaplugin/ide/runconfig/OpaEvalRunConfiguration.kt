/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package com.github.vgramer.opaplugin.ide.runconfig

import com.github.vgramer.opaplugin.ide.runconfig.ui.OpaEvalRunCommandEditor
import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.Executor
import com.intellij.execution.ExternalizablePath
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.io.isFile
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths

/**
 * the opa eval configuration
 *
 * we don't use the options mechanism to persist configuration like in tutorial because its create another class a bit
 * painful to maintain and we had some problems on the restore of parameters
 *
 * @link https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations.html
 */
class OpaEvalRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<OpaEvalRunProfileState>(project, factory, name) {

    /**
     * the querry to evaluate
     */
    var query: String? = null

    /**
     * the input to pass to opa eval  (ie option --input <path>)
     */
    var input: Path? = null

    /**
     * the bundle directory to pass to opa eval (ie option -b ) maybe empty if [additionalArgs] contains
     * --data option
     */
    var bundleDir: Path? = null

    /**
     * others arguments to pass to opa eval command (eg -f pretty)
     */
    var additionalArgs: String? = null


    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> = OpaEvalRunCommandEditor(project)


    override fun checkConfiguration() {
        checkConfig()
    }


    private fun checkConfig() {
        // TODO implement more intelligent test when we additionalArgs component will add a real parser (allow auto completion and real parsing of args)
        if (query.isNullOrBlank()) {
            throw RuntimeConfigurationError("Query can not be empty")
        }

        if (input == null || !input!!.isFile()) {
            throw RuntimeConfigurationError("Input must be a path to a file")
        }

        val args = ParametersListUtil.parse(additionalArgs ?: "")


        val noDataArgs = !(args.contains("--data") || args.contains("-d"))
        val noBundleDir = bundleDir == null || bundleDir!!.toString().isEmpty()

        if (noDataArgs && noBundleDir) {
            throw RuntimeConfigurationError("You must either defined a bundle directory or data through Additional args (option -d <path> or --data <path>)")
        }

        bundleDir?.let {
            if (it.isFile()) {
                throw RuntimeConfigurationError("Bundle directory must be a directory")
            }
        }

    }

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState {
        checkConfig()
        return OpaEvalRunProfileState(executionEnvironment, this)
    }

    /**
     * Handle the deserialization of the run configuration (ie read it from .idea/workspace.xml when IDE start or when
     * user want to edit it )
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)

        query = element.readString("query")
        input = element.readPath("input")
        bundleDir = element.readPath("bundledir")
        additionalArgs = element.readString("addtionalargs")
    }

    /**
     * Handle the serialization of the run configuration (ie save it to .idea/workspace.xml when user modify it and
     * click to apply)
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        element.writeString("query", query)
        element.writePath("input", input)
        element.writePath("bundledir", bundleDir)
        element.writeString("addtionalargs", additionalArgs)

    }
}

@VisibleForTesting
fun Element.writeString(name: String, value: String?) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value ?: "")
    addContent(opt)
}


private fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")

@VisibleForTesting
fun Element.writePath(name: String, value: Path?) {
    if (value != null) {
        val s = ExternalizablePath.urlValue(value.toString())
        writeString(name, s)
    }
}

private fun Element.readPath(name: String): Path? {
    return readString(name)?.let { Paths.get(ExternalizablePath.localPathValue(it)) }
}
