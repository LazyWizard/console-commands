@file:JvmName("SystemInfo")
@file:JvmMultifileClass

package org.lazywizard.console.ext

import org.lwjgl.opengl.ATIMeminfo
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.NVXGpuMemoryInfo
import org.lwjgl.opengl.NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX
import org.lwjgl.opengl.NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX
import java.util.*

internal fun trimVendorString(vendor: String) = vendor.split("/".toRegex(), 2)[0]
        .replace(""" ?(series|\(r\)|\(tm\)|opengl engine)""", "")

internal fun getGPUInfo(): GPUInfo {
    val vendor = glGetString(GL_VENDOR).toLowerCase(Locale.ROOT)
    return when {
        vendor.startsWith("nvidia") -> NvidiaGPUInfo()
        vendor.startsWith("ati") || vendor.startsWith("amd") -> ATIGPUInfo()
        vendor.startsWith("intel") -> IntelGPUInfo()
        else -> GenericGPUInfo()
    }
}

internal abstract class GPUInfo {
    abstract fun getFreeVRAM(): Long
    open fun getGPUString(): String = "GPU Model: ${trimVendorString(glGetString(GL_RENDERER))}\n" +
            "Vendor: ${glGetString(GL_VENDOR)}\n" +
            "Driver version: ${glGetString(GL_VERSION)}\n"+
            "Free VRAM: ${toMB(getFreeVRAM())} MB\n"
}

// https://www.khronos.org/registry/OpenGL/extensions/NVX/NVX_gpu_memory_info.txt
internal class NvidiaGPUInfo : GPUInfo() {
    override fun getFreeVRAM(): Long = glGetInteger(NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX) * 1024L;
    override fun getGPUString(): String {
        val dedicatedVram = toMB(glGetInteger(NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX).toLong() * 1024)
        val maxVram = toMB(glGetInteger(NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX).toLong() * 1024)
        return super.getGPUString() + "Dedicated VRAM: $dedicatedVram MB\nMaximum VRAM:   $maxVram MB\n"
    }
}

// https://www.khronos.org/registry/OpenGL/extensions/ATI/ATI_meminfo.txt
internal class ATIGPUInfo : GPUInfo() {
    override fun getFreeVRAM(): Long = glGetInteger(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI) * 1024L
}

// Intel's integrated GPUS share system RAM instead of having dedicated VRAM
internal class IntelGPUInfo : GPUInfo() {
    override fun getFreeVRAM(): Long = Runtime.getRuntime().freeMemory()
}

// Unknown GPU vendor - assume it's absolute crap that uses system RAM
internal class GenericGPUInfo : GPUInfo() {
    override fun getFreeVRAM(): Long = Runtime.getRuntime().freeMemory()
}
