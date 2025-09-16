package org.lazywizard.console.overlay.v2.panels

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.state.AppDriver
import org.dark.shaders.util.ShaderLib
import org.lazywizard.console.overlay.v2.elements.BaseConsoleElement
import org.lazywizard.console.overlay.v2.misc.ReflectionUtils
import org.lazywizard.console.overlay.v2.misc.clearChildren
import org.lazywizard.console.overlay.v2.misc.getParent
import org.lazywizard.console.overlay.v2.misc.getWidth
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20

class ConsoleOverlayPanel : BaseCustomUIPanelPlugin() {

    var parent: CustomPanelAPI

    companion object {
        var instance: ConsoleOverlayPanel? = null
        var shader = 0
    }

    init {
        instance = this

        if (shader == 0) {
            shader = ShaderLib.loadShader(
                Global.getSettings().loadText("data/shaders/cc_baseVertex.shader"),
                Global.getSettings().loadText("data/shaders/cc_blurFragment.shader"))
            if (shader != 0) {
                GL20.glUseProgram(shader)

                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "tex"), 0)

                GL20.glUseProgram(0)
            } else {
                var test = ""
            }
        }

        var state = AppDriver.getInstance().currentState
        var screenPanel = ReflectionUtils.invoke("getScreenPanel", state) as UIPanelAPI

        if (Global.getCurrentState() == GameState.COMBAT) {
            Global.getCombatEngine().isPaused = true
            var engine = Global.getCombatEngine()


            //ReflectionUtils.set("hideHud", state, true) //Has to be hidden, as some elements still render above, somehow.
        }

        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            Global.getSector().isPaused = true;
        }




        parent = Global.getSettings().createCustom(screenPanel.position.width, screenPanel.position.height, null)
        screenPanel.addComponent(parent)
        parent.position.inTL(0f, 0f)

        recreatePanel()
    }

    fun recreatePanel() {
        parent.clearChildren()

        var panel = parent.createCustomPanel(parent.position.width, parent.position.height, this)
        parent.addComponent(panel)
        panel.position.inTL(0f, 0f)

        var element = panel.createUIElement(panel.position.width, panel.position.height, false)
        panel.addUIElement(element)
        element.position.inTL(0f, 0f)

        element.addPara("Test", 0f).position.inTL(30f, 30f)

       /* var advancer = BaseConsoleElement(element, 0f, 0f)
        advancer.onInput {
            for (event in it) {
                if (event.isConsumed) continue
                if (event.isKeyUpEvent && event.eventValue == Keyboard.KEY_ESCAPE) {
                    close()
                    event.consume()
                    continue
                }
                if (event.isKeyboardEvent)
                {
                    event.consume()
                }
            }
        }*/
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        var state = AppDriver.getInstance().currentState
        if (Global.getCurrentState() == GameState.COMBAT) {
            var reticle = ReflectionUtils.get("targetReticleRenderer", state)
            if (reticle != null) {
                var list = ReflectionUtils.get(null, reticle!!,  List::class.java) as MutableList<*>
                list.clear()
            }
        }


    }

    //Prevent input from going towards other windows
    override fun processInput(events: MutableList<InputEventAPI>?) {
        for (event in events!!)
        {
            if (event.isConsumed) continue
            if (event.isKeyUpEvent && event.eventValue == Keyboard.KEY_ESCAPE)
            {
                event.consume()
                close()
                continue
            }
            if (event.isKeyboardEvent)
            {
                event.consume()
            }
            if (event.isMouseMoveEvent || event.isMouseDownEvent || event.isMouseScrollEvent)
            {

                event.consume()
            }
        }
    }

    override fun render(alphaMult: Float) {

        //Screen texture can be unloaded if graphicslib shaders are disabled, causing a blackscreen
        if (ShaderLib.getScreenTexture() != 0) {
            //Shader

            ShaderLib.beginDraw(shader);

            GL20.glUniform2f(GL20.glGetUniformLocation(shader, "resolution"), ShaderLib.getInternalWidth().toFloat(), ShaderLib.getInternalHeight().toFloat())

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderLib.getScreenTexture());

            //Might Fix Incompatibilities with odd drivers
            GL20.glValidateProgram(shader)
            if (GL20.glGetProgrami(shader, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
                ShaderLib.exitDraw()
                return
            }

            GL11.glDisable(GL11.GL_BLEND);
            ShaderLib.screenDraw(ShaderLib.getScreenTexture(), GL13.GL_TEXTURE0 + 0)
            ShaderLib.exitDraw()
        }
    }

    public fun close() {

        var state = AppDriver.getInstance().currentState

        if (Global.getCurrentState() == GameState.COMBAT) {
            Global.getCombatEngine().isPaused = false
            //ReflectionUtils.set("hideHud", state, false)
        }

        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            Global.getSector().isPaused = false;
        }

        instance = null
        parent.getParent()?.removeComponent(parent)
    }

}