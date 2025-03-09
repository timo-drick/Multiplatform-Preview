package de.drick.compose.utils.external_compiler

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.JVMAssertionsMode
import org.jetbrains.kotlin.config.JvmDefaultMode

fun JsonElement.string(key: String): String? =
    jsonObject[key]?.jsonPrimitive?.contentOrNull
fun JsonElement.boolean(key: String): Boolean? =
    jsonObject[key]?.jsonPrimitive?.contentOrNull == "true"
fun JsonElement.stringList(key: String): List<String>? =
    jsonObject[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
fun JsonElement.stringArray(key: String): Array<String>? =
    jsonObject[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toTypedArray()

fun parseJsonToK2JVMCompilerArguments(json: String): K2JVMCompilerArguments = K2JVMCompilerArguments().apply {
    val j = Json.parseToJsonElement(json)
    destination = j.string("destination")
    includeRuntime = j.boolean("includeRuntime") ?: false
    noJdk = j.boolean("noJdk") ?: false
    noStdlib = j.boolean("noStdlib") ?: true
    noReflect = j.boolean("noReflect") ?: true
    moduleName = j.string("moduleName")
    jvmTarget = j.string("jvmTarget")
    javaParameters = j.boolean("javaParameters") ?: false
    useOldBackend = j.boolean("useOldBackend") ?: false
    allowUnstableDependencies = j.boolean("allowUnstableDependencies") ?: false
    doNotClearBindingContext = j.boolean("doNotClearBindingContext") ?: false
    backendThreads = j.string("backendThreads") ?: "1"
    noCallAssertions = j.boolean("noCallAssertions") ?: false
    noReceiverAssertions = j.boolean("noReceiverAssertions") ?: false
    noParamAssertions = j.boolean("noParamAssertions") ?: false
    noOptimize = j.boolean("noOptimize") ?: false
    assertionsMode = j.string("assertionsMode") ?: JVMAssertionsMode.DEFAULT.description
    inheritMultifileParts = j.boolean("inheritMultifileParts") ?: false
    useTypeTable = j.boolean("useTypeTable") ?: false
    useOldClassFilesReading = j.boolean("useOldClassFilesReading") ?: false
    suppressMissingBuiltinsError = j.boolean("suppressMissingBuiltinsError") ?: false
    useJavac = j.boolean("useJavac") ?: false
    compileJava = j.boolean("compileJava") ?: false
    jvmDefault = j.string("jvmDefault") ?: JvmDefaultMode.DISABLE.description
    disableStandardScript = j.boolean("disableStandardScript") ?: false
    strictMetadataVersionSemantics = j.boolean("strictMetadataVersionSemantics") ?: false
    sanitizeParentheses = j.boolean("sanitizeParentheses") ?: false
    allowNoSourceFiles = j.boolean("allowNoSourceFiles") ?: true
    emitJvmTypeAnnotations = j.boolean("emitJvmTypeAnnotations") ?: false
    noResetJarTimestamps = j.boolean("noResetJarTimestamps") ?: false
    noUnifiedNullChecks = j.boolean("noUnifiedNullChecks") ?: false
    noSourceDebugExtension = j.boolean("noSourceDebugExtension") ?: false
    useOldInlineClassesManglingScheme = j.boolean("useOldInlineClassesManglingScheme") ?: false
    enableJvmPreview = j.boolean("enableJvmPreview") ?: false
    suppressDeprecatedJvmTargetWarning = j.boolean("suppressDeprecatedJvmTargetWarning") ?: false
    typeEnhancementImprovementsInStrictMode = j.boolean("typeEnhancementImprovementsInStrictMode") ?: false
    serializeIr = j.string("serializeIr") ?: "none"
    validateBytecode = j.boolean("validateBytecode") ?: false
    enhanceTypeParameterTypesToDefNotNull = j.boolean("enhanceTypeParameterTypesToDefNotNull") ?: false
    linkViaSignatures = j.boolean("linkViaSignatures") ?: false
    enableDebugMode = j.boolean("enableDebugMode") ?: false
    noNewJavaAnnotationTargets = j.boolean("noNewJavaAnnotationTargets") ?: false
    oldInnerClassesLogic = j.boolean("oldInnerClassesLogic") ?: false
    valueClasses = j.boolean("valueClasses") ?: false
    enableIrInliner = j.boolean("enableIrInliner") ?: false
    useInlineScopesNumbers = j.boolean("useInlineScopesNumbers") ?: false
    //useK2Kapt = j.boolean("useK2Kapt") ?: false
    autoAdvanceLanguageVersion = j.boolean("autoAdvanceLanguageVersion") ?: true
    languageVersion = j.string("languageVersion")
    autoAdvanceApiVersion = j.boolean("autoAdvanceApiVersion") ?: true
    apiVersion = j.string("apiVersion")
    progressiveMode = j.boolean("progressiveMode") ?: false
    script = j.boolean("script") ?: false
    noInline = j.boolean("noInline") ?: false
    skipMetadataVersionCheck = j.boolean("skipMetadataVersionCheck") ?: false
    skipPrereleaseCheck = j.boolean("skipPrereleaseCheck") ?: false
    allowKotlinPackage = j.boolean("allowKotlinPackage") ?: false
    stdlibCompilation = j.boolean("stdlibCompilation") ?: false
    reportOutputFiles = j.boolean("reportOutputFiles") ?: false
    pluginClasspaths = j.stringArray("pluginClasspaths")
    pluginOptions = j.stringArray("pluginOptions")
    multiPlatform = j.boolean("multiPlatform") ?: false
    noCheckActual = j.boolean("noCheckActual") ?: false
    newInference = j.boolean("newInference") ?: false
    inlineClasses = j.boolean("inlineClasses") ?: false
    legacySmartCastAfterTry = j.boolean("legacySmartCastAfterTry") ?: false
    reportPerf = j.boolean("reportPerf") ?: false
    listPhases = j.boolean("listPhases") ?: false
    verifyIrVisibility = j.boolean("verifyIrVisibility") ?: false
    verifyIrVisibilityAfterInlining = j.boolean("verifyIrVisibilityAfterInlining") ?: false
    profilePhases = j.boolean("profilePhases") ?: false
    checkPhaseConditions = j.boolean("checkPhaseConditions") ?: false
    checkStickyPhaseConditions = j.boolean("checkStickyPhaseConditions") ?: false
    useK2 = j.boolean("useK2") ?: false
    //useFirExperimentalCheckers = j.boolean("useFirExperimentalCheckers") ?: false
    useFirIC = j.boolean("useFirIC") ?: false
    useFirLT = j.boolean("useFirLT") ?: true
    metadataKlib = j.boolean("metadataKlib") ?: false
    disableDefaultScriptingPlugin = j.boolean("disableDefaultScriptingPlugin") ?: false
    explicitApi = j.string("explicitApi") ?: ExplicitApiMode.DISABLED.state
    explicitReturnTypes = j.string("explicitReturnTypes") ?: ExplicitApiMode.DISABLED.state
    inferenceCompatibility = j.boolean("inferenceCompatibility") ?: false
    suppressVersionWarnings = j.boolean("suppressVersionWarnings") ?: false
    suppressApiVersionGreaterThanLanguageVersionError = j.boolean("suppressApiVersionGreaterThanLanguageVersionError") ?: false
    extendedCompilerChecks = j.boolean("extendedCompilerChecks") ?: false
    expectActualClasses = j.boolean("expectActualClasses") ?: false
    consistentDataClassCopyVisibility = j.boolean("consistentDataClassCopyVisibility") ?: false
    unrestrictedBuilderInference = j.boolean("unrestrictedBuilderInference") ?: false
    enableBuilderInference = j.boolean("enableBuilderInference") ?: false
    selfUpperBoundInference = j.boolean("selfUpperBoundInference") ?: false
    contextReceivers = j.boolean("contextReceivers") ?: false
    //nonLocalBreakContinue = j.boolean("nonLocalBreakContinue") ?: false
    //directJavaActualization = j.boolean("directJavaActualization") ?: false
    multiDollarInterpolation = j.boolean("multiDollarInterpolation") ?: false
    renderInternalDiagnosticNames = j.boolean("renderInternalDiagnosticNames") ?: false
    allowAnyScriptsInSourceRoots = j.boolean("allowAnyScriptsInSourceRoots") ?: false
    reportAllWarnings = j.boolean("reportAllWarnings") ?: false
    fragments = j.stringArray("fragments")
    fragmentRefines = j.stringArray("fragmentRefines")
    ignoreConstOptimizationErrors = j.boolean("ignoreConstOptimizationErrors") ?: false
    dontWarnOnErrorSuppression = j.boolean("dontWarnOnErrorSuppression") ?: false
    whenGuards = j.boolean("whenGuards") ?: false
    freeArgs = j.stringList("freeArgs") ?: emptyList()
    help = j.boolean("help") ?: false
    extraHelp = j.boolean("extraHelp") ?: false
    version = j.boolean("version") ?: false
    verbose = j.boolean("verbose") ?: false
    suppressWarnings = j.boolean("suppressWarnings") ?: false
    allWarningsAsErrors = j.boolean("allWarningsAsErrors") ?: false
    //extraWarnings = j.boolean("extraWarnings") ?: false
    //internalArguments = j.string("internalArguments")?.split(",") ?: emptyList()
}


@Language("JSON")
val argumentsJson = """
{
  "destination": "/home/timo/compose/github/compose_desktop_dev_challenge/shared/build/classes/kotlin/jvm/main",
  "includeRuntime": false,
  "noJdk": false,
  "noStdlib": true,
  "noReflect": true,
  "moduleName": "shared",
  "jvmTarget": "17",
  "javaParameters": false,
  "useOldBackend": false,
  "allowUnstableDependencies": false,
  "doNotClearBindingContext": false,
  "backendThreads": "1",
  "noCallAssertions": false,
  "noReceiverAssertions": false,
  "noParamAssertions": false,
  "noOptimize": false,
  "assertionsMode": "legacy",
  "inheritMultifileParts": false,
  "useTypeTable": false,
  "useOldClassFilesReading": false,
  "suppressMissingBuiltinsError": false,
  "useJavac": false,
  "compileJava": false,
  "jvmDefault": "disable",
  "disableStandardScript": false,
  "strictMetadataVersionSemantics": false,
  "sanitizeParentheses": false,
  "allowNoSourceFiles": true,
  "emitJvmTypeAnnotations": false,
  "noResetJarTimestamps": false,
  "noUnifiedNullChecks": false,
  "noSourceDebugExtension": false,
  "useOldInlineClassesManglingScheme": false,
  "enableJvmPreview": false,
  "suppressDeprecatedJvmTargetWarning": false,
  "typeEnhancementImprovementsInStrictMode": false,
  "serializeIr": "none",
  "validateBytecode": false,
  "enhanceTypeParameterTypesToDefNotNull": false,
  "linkViaSignatures": false,
  "enableDebugMode": false,
  "noNewJavaAnnotationTargets": false,
  "oldInnerClassesLogic": false,
  "valueClasses": false,
  "enableIrInliner": false,
  "useInlineScopesNumbers": false,
  "useK2Kapt": false,
  "autoAdvanceLanguageVersion": false,
  "languageVersion": "2.1",
  "autoAdvanceApiVersion": false,
  "apiVersion": "2.1",
  "progressiveMode": false,
  "script": false,
  "noInline": false,
  "skipMetadataVersionCheck": false,
  "skipPrereleaseCheck": false,
  "allowKotlinPackage": false,
  "stdlibCompilation": false,
  "reportOutputFiles": false,
  "pluginClasspaths": [
    "/home/timo/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.0/2db69adb9c3ffe970b95e9ba54b92e858a071444/kotlin-compose-compiler-plugin-embeddable-2.1.0.jar",
    "/home/timo/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-jvm/2.1.0/63671ebf34dac00ac97885f3e0e6f7478537bd20/kotlin-scripting-jvm-2.1.0.jar",
    "/home/timo/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-common/2.1.0/269ce38f2d7214d4608059b4326b1710f82b91b9/kotlin-scripting-common-2.1.0.jar",
    "/home/timo/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.1.0/85f8b81009cda5890e54ba67d64b5e599c645020/kotlin-stdlib-2.1.0.jar",
    "/home/timo/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar",
    "/home/timo/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/2.1.0/5909da8c57b75a117714ab0e348c86101b7a3284/kotlin-script-runtime-2.1.0.jar"
  ],
  "pluginOptions": [
    "plugin:androidx.compose.compiler.plugins.kotlin:generateFunctionKeyMetaClasses\u003dfalse",
    "plugin:androidx.compose.compiler.plugins.kotlin:traceMarkersEnabled\u003dtrue"
  ],
  "multiPlatform": true,
  "noCheckActual": false,
  "newInference": false,
  "inlineClasses": false,
  "legacySmartCastAfterTry": false,
  "reportPerf": false,
  "listPhases": false,
  "verifyIrVisibility": false,
  "verifyIrVisibilityAfterInlining": false,
  "profilePhases": false,
  "checkPhaseConditions": false,
  "checkStickyPhaseConditions": false,
  "useK2": false,
  "useFirExperimentalCheckers": false,
  "useFirIC": false,
  "useFirLT": true,
  "metadataKlib": false,
  "disableDefaultScriptingPlugin": false,
  "explicitApi": "disable",
  "explicitReturnTypes": "disable",
  "inferenceCompatibility": false,
  "suppressVersionWarnings": false,
  "suppressApiVersionGreaterThanLanguageVersionError": false,
  "extendedCompilerChecks": false,
  "expectActualClasses": false,
  "consistentDataClassCopyVisibility": false,
  "unrestrictedBuilderInference": false,
  "enableBuilderInference": false,
  "selfUpperBoundInference": false,
  "contextReceivers": false,
  "nonLocalBreakContinue": false,
  "directJavaActualization": false,
  "multiDollarInterpolation": false,
  "renderInternalDiagnosticNames": false,
  "allowAnyScriptsInSourceRoots": false,
  "reportAllWarnings": false,
  "fragments": [
    "jvmMain",
    "commonMain"
  ],
  "fragmentRefines": [
    "jvmMain:commonMain"
  ],
  "ignoreConstOptimizationErrors": false,
  "dontWarnOnErrorSuppression": false,
  "whenGuards": false,
  "freeArgs": [],
  "help": false,
  "extraHelp": false,
  "version": false,
  "verbose": false,
  "suppressWarnings": false,
  "allWarningsAsErrors": false,
  "extraWarnings": false,
  "internalArguments": [],
  "frozen": false
}
""".trimIndent()