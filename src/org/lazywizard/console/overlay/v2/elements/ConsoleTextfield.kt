package org.lazywizard.console.overlay.v2.elements

import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import java.awt.Color

class ConsoleTextfield(tooltip: TooltipMakerAPI, width: Float, height: Float) : BaseConsoleElement(tooltip, width, height) {

    init {

        enableTransparency = true
        renderBorder = false
        borderAlpha = 1f
        backgroundAlpha = 0.85f
        backgroundColor = Color(20, 20, 25)

       /* innerElement.setParaFont(Fonts.ORBITRON_24AABOLD)
        var arrow = innerElement.addPara(">", 0f, Misc.getBasePlayerColor().darker(), Misc.getBasePlayerColor().darker())
        arrow.position.inTL(12f, 11f)
        innerElement.setParaFont(Fonts.ORBITRON_16)*/

    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

        renderLine(alphaMult)
    }

    fun renderLine(alphaMult: Float) {
        var c = borderColor
        GL11.glPushMatrix()

        GL11.glTranslatef(0f, 0f, 0f)
        GL11.glRotatef(0f, 0f, 0f, 1f)

        GL11.glDisable(GL11.GL_TEXTURE_2D)

        if (enableTransparency)
        {
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        }
        else
        {
            GL11.glDisable(GL11.GL_BLEND)
        }

        GL11.glColor4f(c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (alphaMult * borderAlpha))

        GL11.glLineWidth(3f)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glBegin(GL11.GL_LINE_STRIP)


        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x + width, y)


        GL11.glEnd()
        GL11.glPopMatrix()

        GL11.glLineWidth(1f)

    }

}