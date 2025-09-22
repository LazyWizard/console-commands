package org.lazywizard.console.overlay.v2.misc

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI

fun UIPanelAPI.getChildrenCopy() : List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
}

fun UIPanelAPI.getChildrenNonCopy() : List<UIComponentAPI>  {
    return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
}

fun UIComponentAPI.getParent() : UIPanelAPI?  {
    return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
}

fun TooltipMakerAPI.getParentWidget() : UIComponentAPI? {
    return ReflectionUtils.invoke("getParentWidget", this) as UIPanelAPI
}

fun UIComponentAPI.setOpacity(alpha: Float)
{
    ReflectionUtils.invoke("setOpacity", this, alpha)
}

fun UIPanelAPI.clearChildren() {
   ReflectionUtils.invoke("clearChildren", this)
}

fun UIComponentAPI.getWidth() : Float  {
    return ReflectionUtils.invoke("getWidth", this) as Float
}

//Code Below created by Starficz

// CustomPanelAPI implements the same Listener that a ButtonAPI requires,
// A CustomPanel then happens to trigger its CustomUIPanelPlugin buttonPressed() method
// thus we can map our functions into a CustomUIPanelPlugin, and have them be triggered
private class ButtonListener(var button: ButtonAPI) : BaseCustomUIPanelPlugin() {
    private val onClickFunctions = mutableListOf<() -> Unit>()

    var buttonListener = Global.getSettings().createCustom(0f, 0f, this)

    init {
        /*val setListenerMethod = ReflectionUtils.getMethodsOfName("setListener", button)[0]
        ReflectionUtils.rawInvoke(setListenerMethod, button, buttonListener)*/

        var method = ReflectionUtils.getMethod("setListener", button.javaClass, null, null, null)
        method!!.invoke(button, buttonListener)

        //ReflectionUtils.invoke("setListener", button, buttonListener)

    }

    override fun buttonPressed(buttonId: Any?) {
        onClickFunctions.forEach { it() }
    }

    fun addOnClick(function: () -> Unit) {
        onClickFunctions.add(function)
    }
}

// Extension function for ButtonAPI
internal fun ButtonAPI.onClick(function: () -> Unit) {
    // Use reflection to check if this button already has a listener
    val existingListener = ReflectionUtils.invoke("getListener", this)
    if (existingListener is ButtonListener) {
        existingListener.addOnClick(function)
    } else {
        // if not, make one
        val listener = ButtonListener(this)
        listener.addOnClick(function)
    }
}
