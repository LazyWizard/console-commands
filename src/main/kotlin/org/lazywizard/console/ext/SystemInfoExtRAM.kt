@file:JvmName("SystemInfo")
@file:JvmMultifileClass

package org.lazywizard.console.ext

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

internal fun getMemoryInfo(): MemoryInfo {
    if (System.getProperty("java.vm.name").contains("hotspot", true))
        return SunMemoryInfo()
    return MemoryInfo()
}

internal open class MemoryInfo {
    open fun getRAMString(): String = with(Runtime.getRuntime())
    {
        "JVM RAM: ${toMB(this.totalMemory() - this.freeMemory())} MB / ${toMB(this.totalMemory())} " +
                "(${toMB(this.freeMemory())} MB free, ${toMB(this.maxMemory())} MB allocatable)\n"
    }
}

internal class SunMemoryInfo : MemoryInfo() {
    override fun getRAMString(): String =
            super.getRAMString() + with(ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean)
            {
                "System RAM: ${toMB(this.freePhysicalMemorySize)} MB remaining, " +
                        "${toMB(this.totalPhysicalMemorySize)} MB total\n"
            }
}
