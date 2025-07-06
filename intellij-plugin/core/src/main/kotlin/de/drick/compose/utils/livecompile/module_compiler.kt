package de.drick.compose.utils.livecompile

import de.drick.compose.hotpreview.plugin.runCatchingCancellationAware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.measureTime

data class SourceSet(
    val commonSrcDir: String? = null,
    val jvmSrcDir: String? = null
)

fun hotRecompileFlow(
    compile: suspend () -> Unit = {},
    targetJvmVersion: String,
    jdkHome: String,
    classPath: List<String>,
    sourceSet: SourceSet,
    outputFolder: File,
): Flow<Int> = flow {
    /*val compiler = KotlinLiveCompiler(
        targetJvmVersion = targetJvmVersion,
        systemClassPath = classPath,
        outputFolder = outputFolder
    )
    compiler.initialisation()*/
    val directories = listOfNotNull(sourceSet.commonSrcDir, sourceSet.jvmSrcDir).map { File(it) }
    val kotlinFileChangeFlow = directoryMonitorFlow(directories).filterKotlinFiles()
    var compileCounter = 0
    kotlinFileChangeFlow.collect { changedFileList ->
        println("File changes: $changedFileList")

        // TODO Find a reliable way to only recompile changed files

        // compile modules where files where changed

        //Collect all files
        val desktopFileList = sourceSet.jvmSrcDir?.let { dir ->
            File(dir).walkTopDown().filter { it.extension == "kt" }.toList()
        } ?: emptyList()

        val commonFileList = sourceSet.commonSrcDir?.let { dir ->
            File(dir).walkTopDown().filter { it.extension == "kt" }.toList()
        } ?: emptyList()

        val changedSet = changedFileList.map { it.file }.toSet()
        if (changedSet.containsAny(desktopFileList) || changedSet.containsAny(commonFileList)) {
            //Recompile
            withContext(Dispatchers.Default) {
                runCatchingCancellationAware {
                    println("Compiling started: $sourceSet")
                    val compileTime = measureTime {
                        compile()
                        /*compiler.compile(
                            fileList = desktopFileList + commonFileList,//changedFileList.map { it.file },
                            commonSrcFileList = commonFileList,
                            jdkHomeString = jdkHome
                            //module = index.toString()
                        )*/
                    }
                    println("Compiling end duration: $compileTime")
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
        //TODO inform subscriber
        compileCounter++
        emit(compileCounter)
    }
}


fun <T> Collection<T>.containsAny(other: Collection<T>): Boolean {
    // Use HashSet instead of #toSet which uses a LinkedHashSet
    val set = if (this is Set) this else HashSet(this)
    for (item in other)
        if (set.contains(item)) // early return
            return true
    return false
}