@file:JvmName("SystemInfo")
@file:JvmMultifileClass

package org.lazywizard.console.ext

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import org.lwjgl.LWJGLUtil
import org.lwjgl.Sys
import org.lwjgl.opengl.Display
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

internal fun toMB(bytes: Long): String = DecimalFormat("#,###.00").format(bytes / (1024f * 1024f))

internal fun getGameVersionString(): String = "Game version: ${Global.getSettings().versionString}\n"

internal fun getModString(mod: ModSpecAPI): String = "${mod.name} ${mod.version} by ${mod.author}\n"

internal fun getPlatformString(): String = "Java version: ${System.getProperty("java.runtime.name")} " +
        "${System.getProperty("java.runtime.version")} (${System.getProperty("java.vm.name")})\n" +
        "Platform: ${LWJGLUtil.getPlatformName().capitalize()} (${if (Sys.is64Bit()) "64" else "32"}-bit)\n"

internal fun getLaunchArgsString(): String = "Launch args: " +
        ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(separator = " ") + "\n"

internal fun getDisplayString(): String {
    val displayMode = Display.getDisplayMode()
    return "Game resolution: ${Display.getWidth()}x${Display.getHeight()} (${displayMode.frequency}hz, " +
            "${displayMode.bitsPerPixel}bpp, ${if (Display.isFullscreen()) "fullscreen" else "windowed"})\n"
}

internal fun getRAMString(): String = getMemoryInfo().getRAMString()

internal fun getGPUString(): String = getGPUInfo().getGPUString()
