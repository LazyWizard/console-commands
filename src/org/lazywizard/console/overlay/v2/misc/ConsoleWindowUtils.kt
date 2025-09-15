package org.lazywizard.console.overlay.v2.misc

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.state.AppDriver
import org.lazywizard.console.overlay.v2.elements.BaseConsoleElement
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

object ConsoleWindowUtils {

    var popupPanelParent: CustomPanelAPI? = null
    var popupPanelBackground: BaseConsoleElement? = null
    var increase = true
    var isClosing = false
    fun createPopupPanel(width: Float, height: Float, lambda: (TooltipMakerAPI) -> Unit) {
        if (popupPanelParent != null) return //Dont let it open twice

        var state = AppDriver.getInstance().currentState
        var screenPanel = ReflectionUtils.get("screenPanel", state) as UIPanelAPI

        var sW = Global.getSettings().screenWidth
        var sH = Global.getSettings().screenHeight

        popupPanelParent = Global.getSettings().createCustom(sW, sH, null)
        screenPanel.addComponent(popupPanelParent)
        popupPanelParent!!.position.inTL(0f, 0f)

        var element = popupPanelParent!!.createUIElement(sW, sH, false)
        popupPanelParent!!.addUIElement(element)

        var opacity = 0f

        popupPanelBackground = BaseConsoleElement(element, sW, sH).apply {
            enableTransparency = true
            renderBorder = false
            backgroundColor = Color(0, 0, 0)
            backgroundAlpha = 0.5f

            advance {
                if (increase) {
                    opacity += 5 * it
                    if (opacity >= 1f) {
                        opacity = MathUtils.clamp(opacity, 0f, 1f)
                        increase = false
                    }
                    popupPanelParent!!.setOpacity(opacity)
                }
            }

            onInput {
                for (event in it) {
                    if (event.isConsumed) continue
                    if (event.isKeyDownEvent && event.eventValue == Keyboard.KEY_ESCAPE) {
                        closePopupPanel()
                        event.consume()
                        continue
                    }
                    event.consume()
                }
            }
        }

        popupPanelBackground!!.position.inTL(0f, 0f)

        var panel = Global.getSettings().createCustom(width, height, null)
        popupPanelParent!!.addComponent(panel)
        panel.position.inTL(sW/2-width/2, sH/2-height/2)

        var tooltip = panel.createUIElement(width, height, false)
        panel.addUIElement(tooltip)
        tooltip.position.inTL(0f, 0f)

        var b = BaseConsoleElement(tooltip, 0f, 0f).apply {
            enableTransparency = true
            borderAlpha = 0.7f
            backgroundAlpha = 1f
            backgroundColor = Color(0, 0, 0)
        }

        lambda(tooltip)

        b.position.setSize(width, height)

        popupPanelParent!!.setOpacity(0f)

    }

    fun closePopupPanel() {

        if (isClosing) return

        isClosing = true

        var state = AppDriver.getInstance().currentState
        var screenPanel = ReflectionUtils.get("screenPanel", state) as UIPanelAPI

        var opacity = 1f

        popupPanelBackground!!.advance {
            if (increase) return@advance
            opacity -= 5 * it
            popupPanelParent!!.setOpacity(opacity)

            if (opacity <= 0f) {
                screenPanel.removeComponent(popupPanelParent)
                popupPanelParent = null
                popupPanelBackground = null
                increase = true
                isClosing = false
            }
        }
    }

}