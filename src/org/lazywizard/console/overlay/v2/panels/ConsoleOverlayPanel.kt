package org.lazywizard.console.overlay.v2.panels

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.state.AppDriver
import org.dark.shaders.util.ShaderLib
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.overlay.v2.elements.ConsoleTextfield
import org.lazywizard.console.overlay.v2.misc.ReflectionUtils
import org.lazywizard.console.overlay.v2.misc.clearChildren
import org.lazywizard.console.overlay.v2.misc.getParent
import org.lazywizard.lazylib.ui.LazyFont
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_ONE
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class ConsoleOverlayPanel : BaseCustomUIPanelPlugin() {

    var parent: CustomPanelAPI

    companion object {
        var instance: ConsoleOverlayPanel? = null
        var shader = 0
        var font1 = LazyFont.loadFont("graphics/fonts/jetbrains_mono_24.fnt")
        var font2 = LazyFont.loadFont("graphics/fonts/jetbrains_mono_32.fnt")
    }

    var input = ""
    var lastInput = ""
    var currentIndex = 0

    var inputDraw = font1.createText("", Color.white, 16f)
    var arrowDraw = font2.createText(">", Misc.getBasePlayerColor(), 24f)

    init {
        inputDraw.blendSrc = GL_ONE
        inputDraw.renderDebugBounds = false
        arrowDraw.blendSrc = GL_ONE

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

        var width = parent.position.width
        var height = parent.position.height
        var widthOffset = 50

        var panel = parent.createCustomPanel(width, height, this)
        parent.addComponent(panel)
        panel.position.inTL(0f, 0f)

        var element = panel.createUIElement(width, height, false)
        panel.addUIElement(element)
        element.position.inTL(0f, 0f)

        createTextfield(panel, element)

    }

    fun createTextfield(panel: UIPanelAPI, element: TooltipMakerAPI) {

        var width = parent.position.width
        var height = parent.position.height
        var widthOffset = 50

        var font = Fonts.ORBITRON_16
        //var content = "Hello " +    "A ".repeat(300) +"A "
        var content = input

        var innerSizeReduction = 30f

        //Create a dummy container that is used to determine the required size
       /* var testTextContainer = BaseConsoleElement(element, width-widthOffset-innerSizeReduction, 30f)
        testTextContainer.innerElement.setParaFont(font)
        var label1 = testTextContainer.innerElement.addPara(content, 0f)
        testTextContainer.position.inTL(100000f, 100000f) //Removing it doesnt work for some reason*/


        inputDraw.text = input
        inputDraw.baseColor = Color.white
        if (input.isEmpty()) {
            inputDraw.baseColor = Misc.getGrayColor()
            inputDraw.text = CommonStrings.INPUT_QUERY
        }
        inputDraw.maxWidth = width-widthOffset-innerSizeReduction
        inputDraw.triggerRebuildIfNeeded()
        var textWidth = inputDraw.width
        var textHeight = Math.max(inputDraw.height, inputDraw.fontSize) //Height is 0 if theres no characters yet

        //var textfield = ConsoleTextfield(element, width-widthOffset, 30f+label1.position.height)
        var textfield = ConsoleTextfield(element, width-widthOffset, 30f+textHeight)
       /* var textContainer = BaseConsoleElement(textfield.innerElement, textfield.position.width-innerSizeReduction, textfield.height).apply {
            renderBorder = false
            renderBackground = false
            innerElement.setParaFont(font)
        }
        textContainer.position.inTL(innerSizeReduction, 14f)*/
        textfield.position.inTL(width/2-textfield.width/2, height-textfield.height-30)

        textfield.render {
            arrowDraw.draw(textfield.x+10, textfield.y+textfield.height-10)
            inputDraw.draw(textfield.x+innerSizeReduction, textfield.y+textfield.height-15)
        }

        /*var label2 = textContainer.innerElement.addPara(content, 0f)
        label2.position.inTL(0f, 0f)*/

        //println("Width: "+label2.computeTextWidth(label2.text))
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        //Required to hide the hull/flux widget, as it exists outside of the normal UI tree
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
    override fun processInput(events: MutableList<InputEventAPI>) {

        for (event in events) {
            if (event.isConsumed) continue
            //if (!event.isKeyDownEvent) continue
            if (event.isKeyboardEvent && (event.isKeyDownEvent || event.isRepeat))
            {

                if (event.eventValue == Keyboard.KEY_TAB)
                {

                }

                if (event.eventValue == Keyboard.KEY_V && event.isCtrlDown)
                {
                    var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor) as String
                    for (char in clipboard)
                    {
                        //appendCharIfPossible(char)
                        input += char
                    }
                    event.consume()
                    break
                }

                if (event.eventValue == Keyboard.KEY_BACK)
                {
                    var empty = input.isEmpty()

                    if (empty)
                    {
                        event.consume()
                    }
                    else if (event.isShiftDown)
                    {
                        input = ""
                    }
                    /*else if (event.isCtrlDown)
                    {
                        deleteLastWord()
                    }*/
                    else
                    {
                        //playSound("ui_typer_type")
                        input = (input.substring(0, input.length - 1))
                    }


                    event.consume()
                    continue
                }

                if (event.isCtrlDown || event.isAltDown)
                {
                    event.consume()
                    continue
                }

                if (event.eventValue == Keyboard.KEY_RETURN) {
                    event.consume()

                    if (event.isShiftDown) {
                        input += "\n"
                    }
                    continue
                }


                var char = event.eventChar

                if (isValidChar(char)) {
                    input += char
                    event.consume()
                }

                //appendCharIfPossible(char)

            }
        }

        if (input != lastInput) {
            lastInput = input
            recreatePanel()
        }

        //Consume unconsumed keys to prevent interacting with other elements
        for (event in events)
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

    private fun isValidChar(char: Char?) : Boolean
    {
        if (char == '\u0000')
        {
            return false
        }

        else if (char == '%')
        {
            return false
        }
        else if (char == '$')
        {
            return false
        }

        return true
    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

    }

    override fun renderBelow(alphaMult: Float) {

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