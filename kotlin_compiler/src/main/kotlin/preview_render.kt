package de.drick.compose.hotpreview

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services


class HotPreviewCompiler(
    private val jvmTarget: String,
    private val languageVersion: String
) {
    private val compiler = K2JVMCompiler()
    private val baseArguments = K2JVMCompilerArguments().also {
        it.jvmTarget = jvmTarget
        it.languageVersion = languageVersion
    }

    /*fun compile(
        classPaths: List<String>
    ) {
        println("Class path:")
        classPaths.forEach {
            println(it)
        }
    }*/

    fun compile(
        module: String?,
        destFolder: String,
        classPaths: List<String>,
        fileList: List<String>
    ) {
        val compilerArgs = baseArguments.apply {
            destination = destFolder
            classpath = classPaths.joinToString(":")
            freeArgs = fileList
            moduleName = module

            /*
            fragmentSources = null
            fragments = null
            fragmentRefines = null
            */

            noStdlib = true
            noReflect = true
            noJdk = true

            suppressWarnings = true
            incrementalCompilation = true
            noOptimize = true
            noCheckActual = true
        }
        println("Classpath:")
        println(classPaths.joinToString(":"))
        println("Compiling ${fileList.joinToString(",")}")
        try {
            compiler.exec(messageCollector, Services.EMPTY, compilerArgs)
        } catch (err: Throwable) {
            err.printStackTrace()
        }
        println("Compiling finished")
    }
}

private val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)
