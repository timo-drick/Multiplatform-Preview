package de.drick.compose.utils.livecompile

/*import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTime

private val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)

class KotlinLiveCompiler(
    private val targetJvmVersion: String = System.getProperty("java.vm.specification.version"),
    systemClassPath: List<String> = System.getProperty("java.class.path")
        .split(File.pathSeparator)
        .filter {
            File(it).exists() && File(it).canRead()
        }
        .filter { it.endsWith(".jar") },
    private val outputFolder: File
) {
    private val destinationFolder = "$outputFolder"
    private val classPath = (systemClassPath + destinationFolder).joinToString(":")
    private var pluginPathList = arrayOf<String>()
    private val compiler = K2JVMCompiler()


    suspend fun initialisation() = withContext(Dispatchers.IO) {
        deleteDir(outputFolder)
        outputFolder.mkdirs()
        File(destinationFolder).mkdirs()
        //copyResourceFolder(outputFolder, "compilerPlugins")
        pluginPathList = /*File("$outputFolder/compilerPlugins").listFiles()
            .map { it.toString() }
            .toTypedArray()*/
            emptyArray()
        pluginPathList.forEach {
            println("Compiler plugin path: $it")
        }
    }

    suspend fun compile(
        fileList: List<File>,
        commonSrcFileList: List<File>,
        module: String? = null,
        jdkHomeString: String? = null
    ) = withContext(Dispatchers.Default) {
        val compilerArgs = K2JVMCompilerArguments().apply {
            languageVersion = "2.0"
            jdkHome = jdkHomeString
            moduleName = module
            freeArgs = fileList.map { it.path }
            commonSources = commonSrcFileList.map { it.path }.toTypedArray()
            friendPaths = arrayOf(destinationFolder)
            destination = destinationFolder
            classpath = classPath
            pluginClasspaths = pluginPathList
            noJdk = false
            noReflect = true
            noStdlib = true
            jvmTarget = targetJvmVersion
            script = false
            noOptimize = true
            reportPerf = false
            incrementalCompilation = true
            disableDefaultScriptingPlugin = true
            disableStandardScript = true

            multiPlatform = true
            noCheckActual = true
        }
        //Recompile

        try {
            println("Compiling files: ${fileList.joinToString { it.name }}")
            val compileTime = measureTime {
                compiler.exec(messageCollector, Services.EMPTY, compilerArgs)
            }
            println("Compile time: $compileTime")
        } catch (err: Throwable) {
            if (err is CancellationException) {
                println("Compilation cancelled")
                throw err
            }
            err.printStackTrace()
        }
    }
}

fun deleteDir(file: File) {
    file.listFiles()?.let { contents ->
        contents.forEach {
            deleteDir(it)
        }
    }
    file.delete()
}

*/