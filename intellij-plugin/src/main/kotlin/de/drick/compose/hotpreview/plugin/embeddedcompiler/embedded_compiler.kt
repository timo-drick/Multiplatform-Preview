package de.drick.compose.hotpreview.plugin.embeddedcompiler

/*import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services

fun compileMultiplatform(
    compiler: K2JVMCompiler,
    baseArguments: K2JVMCompilerArguments,
    module: String?,
    destFolder: String,
    classPaths: List<String>,
    jvmSources: List<String>,
    commonSources: List<String>,
) {
    val compilerArgs = baseArguments.apply {
        destination = destFolder
        //commonSources = sourcePathCommon
        classpath = classPaths.joinToString(":")
        freeArgs = jvmSources + commonSources
        moduleName = module

        fragmentSources = (jvmSources.map { "jvmMain:$it" } + commonSources.map { "commonMain:$it" }).toTypedArray()
        // TODO maybe fragments can be just taken from the baseArguments
        fragments = arrayOf(
            "jvmMain",
            "commonMain"
        )
        fragmentRefines = arrayOf(
            "jvmMain:commonMain"
        )
        suppressWarnings = true
        incrementalCompilation = true
        noOptimize = true
        backendThreads = "4"
    }

    compiler.exec(messageCollector, Services.EMPTY, compilerArgs)
}

fun compileMultiplatformSingle(
    compiler: K2JVMCompiler,
    baseArguments: K2JVMCompilerArguments,
    module: String?,
    destFolder: String,
    classPaths: List<String>,
    singleFile: String
) {
    val classPathList = classPaths + destFolder
    val compilerArgs = baseArguments.apply {
        destination = "runtime2"
        //friendPaths = arrayOf(destFolder)
        //commonSources = arrayOf(singleFile)
        classpath = classPathList.joinToString(":")
        freeArgs = listOf(singleFile)
        fragmentSources = null//arrayOf("commonMain:$singleFile")
        fragmentRefines = null
        fragments = null
        moduleName = module
        suppressWarnings = true
        incrementalCompilation = false
        disableDefaultScriptingPlugin = true
        disableStandardScript = true
        script = false
        noCheckActual = true
    }

    compiler.exec(messageCollector, Services.EMPTY, compilerArgs)
}

private val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)
*/