package org.lazywizard.console.overlay

import org.apache.log4j.Logger

class LazyFont(val fontName: String, val baseHeight: Float, val textureId: Int,
               val textureWidth: Float, val textureHeight: Float) {
    val Log: Logger = Logger.getLogger(LazyFont::class.java)
    val lookupTable: Array<LazyChar?> = arrayOfNulls<LazyChar>(224)
    val extendedChars = LinkedHashMap<Char, LazyChar>()

    fun addChar(id: Int, tx: Int, ty: Int, width: Int, height: Int, xOffset: Int, yOffset: Int, advance: Int) {
        val tmp = LazyChar(id, tx, ty, width, height, xOffset, yOffset, advance)
        if (tmp.id in 32..255)
            lookupTable[tmp.id - 32] = tmp
        else
            extendedChars[tmp.id.toChar()] = tmp
    }

    fun getChar(character: Char): LazyChar {
        val ch: LazyChar? = if (character in 32..255) lookupTable[character.toInt() - 32] else extendedChars[character]
        return ch ?: getChar('?')
    }

    inner class LazyChar(val id: Int, tx: Int, ty: Int, val width: Int, val height: Int,
                         val xOffset: Int, val yOffset: Int, val advance: Int) {
        val kernings = HashMap<Int, Int>()
        // Internal texture coordinates
        val tx1: Float = tx / textureWidth
        val tx2: Float = tx1 + (width / textureWidth)
        val ty1: Float = (textureHeight - ty) / textureHeight
        val ty2: Float = ty1 - (height / textureHeight)

        fun addKerning(otherChar: Int, kerning: Int) {
            kernings[otherChar] = kerning
        }

        fun getKerning(otherChar: LazyChar?): Int {
            if (otherChar == null)
                return 0

            return kernings.getOrElse(otherChar?.id, { 0 })
        }
    }
}