package de.drick.compose.window_insets_simulation


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icons copied from material-icons-extended library to avoid dependency on it
 */

inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = MaterialIconDimension.dp,
    defaultHeight = MaterialIconDimension.dp,
    viewportWidth = MaterialIconDimension,
    viewportHeight = MaterialIconDimension,
    autoMirror = autoMirror
).block().build()

@PublishedApi
internal const val MaterialIconDimension = 24f

inline fun ImageVector.Builder.materialPath(
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit
) = path(
        fill = SolidColor(Color.Black),
        fillAlpha = fillAlpha,
        stroke = null,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = pathFillType,
        pathBuilder = pathBuilder
    )

val Icons_Filled_Lens: ImageVector
    get() {
        if (_lens != null) {
            return _lens!!
        }
        _lens = materialIcon(name = "Filled.Lens") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
            }
        }
        return _lens!!
    }

private var _lens: ImageVector? = null

val Icons_Filled_Wifi: ImageVector
    get() {
        if (_wifi != null) {
            return _wifi!!
        }
        _wifi = materialIcon(name = "Filled.Wifi") {
            materialPath {
                moveTo(1.0f, 9.0f)
                lineToRelative(2.0f, 2.0f)
                curveToRelative(4.97f, -4.97f, 13.03f, -4.97f, 18.0f, 0.0f)
                lineToRelative(2.0f, -2.0f)
                curveTo(16.93f, 2.93f, 7.08f, 2.93f, 1.0f, 9.0f)
                close()
                moveTo(9.0f, 17.0f)
                lineToRelative(3.0f, 3.0f)
                lineToRelative(3.0f, -3.0f)
                curveToRelative(-1.65f, -1.66f, -4.34f, -1.66f, -6.0f, 0.0f)
                close()
                moveTo(5.0f, 13.0f)
                lineToRelative(2.0f, 2.0f)
                curveToRelative(2.76f, -2.76f, 7.24f, -2.76f, 10.0f, 0.0f)
                lineToRelative(2.0f, -2.0f)
                curveTo(15.14f, 9.14f, 8.87f, 9.14f, 5.0f, 13.0f)
                close()
            }
        }
        return _wifi!!
    }

private var _wifi: ImageVector? = null


val Icons_Filled_BatteryChargingFull: ImageVector
    get() {
        if (_batteryChargingFull != null) {
            return _batteryChargingFull!!
        }
        _batteryChargingFull =
            materialIcon(name = "Filled.BatteryChargingFull") {
                materialPath {
                    moveTo(15.67f, 4.0f)
                    horizontalLineTo(14.0f)
                    verticalLineTo(2.0f)
                    horizontalLineToRelative(-4.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(8.33f)
                    curveTo(7.6f, 4.0f, 7.0f, 4.6f, 7.0f, 5.33f)
                    verticalLineToRelative(15.33f)
                    curveTo(7.0f, 21.4f, 7.6f, 22.0f, 8.33f, 22.0f)
                    horizontalLineToRelative(7.33f)
                    curveToRelative(0.74f, 0.0f, 1.34f, -0.6f, 1.34f, -1.33f)
                    verticalLineTo(5.33f)
                    curveTo(17.0f, 4.6f, 16.4f, 4.0f, 15.67f, 4.0f)
                    close()
                    moveTo(11.0f, 20.0f)
                    verticalLineToRelative(-5.5f)
                    horizontalLineTo(9.0f)
                    lineTo(13.0f, 7.0f)
                    verticalLineToRelative(5.5f)
                    horizontalLineToRelative(2.0f)
                    lineTo(11.0f, 20.0f)
                    close()
                }
            }
        return _batteryChargingFull!!
    }

private var _batteryChargingFull: ImageVector? = null


val Icons_Filled_Minimize: ImageVector
    get() {
        if (_minimize != null) {
            return _minimize!!
        }
        _minimize = materialIcon(name = "Filled.Minimize") {
            materialPath {
                moveTo(6.0f, 19.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(6.0f)
                close()
            }
        }
        return _minimize!!
    }

private var _minimize: ImageVector? = null


val Icons_Filled_Menu: ImageVector
    get() {
        if (_menu != null) {
            return _menu!!
        }
        _menu = materialIcon(name = "Filled.Menu") {
            materialPath {
                moveTo(3.0f, 18.0f)
                horizontalLineToRelative(18.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 16.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 13.0f)
                horizontalLineToRelative(18.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 11.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(18.0f)
                lineTo(21.0f, 6.0f)
                lineTo(3.0f, 6.0f)
                close()
            }
        }
        return _menu!!
    }

private var _menu: ImageVector? = null


val Icons_Filled_Close: ImageVector
    get() {
        if (_close != null) {
            return _close!!
        }
        _close = materialIcon(name = "Filled.Close") {
            materialPath {
                moveTo(19.0f, 6.41f)
                lineTo(17.59f, 5.0f)
                lineTo(12.0f, 10.59f)
                lineTo(6.41f, 5.0f)
                lineTo(5.0f, 6.41f)
                lineTo(10.59f, 12.0f)
                lineTo(5.0f, 17.59f)
                lineTo(6.41f, 19.0f)
                lineTo(12.0f, 13.41f)
                lineTo(17.59f, 19.0f)
                lineTo(19.0f, 17.59f)
                lineTo(13.41f, 12.0f)
                close()
            }
        }
        return _close!!
    }

private var _close: ImageVector? = null


val Icons_AutoMirrored_Filled_ArrowBack: ImageVector
    get() {
        if (_arrowBack != null) {
            return _arrowBack!!
        }
        _arrowBack = materialIcon(
            name = "AutoMirrored.Filled.ArrowBack",
            autoMirror = true
        ) {
            materialPath {
                moveTo(20.0f, 11.0f)
                horizontalLineTo(7.83f)
                lineToRelative(5.59f, -5.59f)
                lineTo(12.0f, 4.0f)
                lineToRelative(-8.0f, 8.0f)
                lineToRelative(8.0f, 8.0f)
                lineToRelative(1.41f, -1.41f)
                lineTo(7.83f, 13.0f)
                horizontalLineTo(20.0f)
                verticalLineToRelative(-2.0f)
                close()
            }
        }
        return _arrowBack!!
    }

private var _arrowBack: ImageVector? = null



val Icons_Navigation_Back: ImageVector
    get() {
        if (_playArrow != null) {
            return _playArrow!!
        }
        _playArrow = materialIcon(name = "Rounded.PlayArrow") {
            materialPath {
                moveTo(8.0f, 6.82f)
                verticalLineToRelative(10.36f)
                curveToRelative(0.0f, 0.79f, 0.87f, 1.27f, 1.54f, 0.84f)
                lineToRelative(8.14f, -5.18f)
                curveToRelative(0.62f, -0.39f, 0.62f, -1.29f, 0.0f, -1.69f)
                lineTo(9.54f, 5.98f)
                curveTo(8.87f, 5.55f, 8.0f, 6.03f, 8.0f, 6.82f)
                close()
            }
        }
        return _playArrow!!
    }

private var _playArrow: ImageVector? = null


val Icons_Filled_Circle: ImageVector
    get() {
        if (_circle != null) {
            return _circle!!
        }
        _circle = materialIcon(name = "Filled.Circle") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.47f, 2.0f, 2.0f, 6.47f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.47f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.47f, 10.0f, -10.0f)
                reflectiveCurveTo(17.53f, 2.0f, 12.0f, 2.0f)
                close()
            }
        }
        return _circle!!
    }

private var _circle: ImageVector? = null


val Icons_Filled_Rectangle: ImageVector
    get() {
        if (_rectangle != null) {
            return _rectangle!!
        }
        _rectangle = materialIcon(name = "Rounded.Rectangle") {
            materialPath {
                moveTo(2.0f, 6.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
                close()
            }
        }
        return _rectangle!!
    }

private var _rectangle: ImageVector? = null
