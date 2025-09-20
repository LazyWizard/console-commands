package org.lazywizard.console.overlay.v2.elements

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.lazywizard.console.overlay.v2.font.ConsoleFont

internal class ConsoleTextElement(var toDraw: ConsoleFont.DrawableString, tooltip: TooltipMakerAPI, width: Float, height: Float) : BaseConsoleElement(tooltip, width, height) {

    init {
        enableTransparency = true
        renderBorder = false
        renderBackground = false
    }

    override fun render(alphaMult: Float) {
        toDraw.draw(x, y)
    }

}