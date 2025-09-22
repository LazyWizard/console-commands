package org.lazywizard.console.overlay.v2.panels

import com.fs.graphics.util.Fader
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.state.AppDriver
import kotlinx.coroutines.*
import org.codehaus.commons.compiler.CompileException
import org.dark.shaders.util.ShaderLib
import org.json.JSONArray
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommandWithSuggestion
import org.lazywizard.console.CommandStore
import org.lazywizard.console.CommandStore.StoredCommand
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console
import org.lazywizard.console.commands.RunCode
import org.lazywizard.console.overlay.v2.elements.BaseConsoleElement
import org.lazywizard.console.overlay.v2.elements.ConsoleTextElement
import org.lazywizard.console.overlay.v2.elements.ConsoleTextfield
import org.lazywizard.console.overlay.v2.font.ConsoleFont
import org.lazywizard.console.overlay.v2.misc.ReflectionUtils
import org.lazywizard.console.overlay.v2.misc.clearChildren
import org.lazywizard.console.overlay.v2.misc.getParent
import org.lazywizard.lazylib.JSONUtils
import org.lazywizard.lazylib.JSONUtils.CommonDataJSONObject
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.Sys
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_ONE
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.util.regex.Pattern

class ConsoleOverlayPanel(private val context: CommandContext) : BaseCustomUIPanelPlugin() {

    var parent: CustomPanelAPI

    companion object {
        @JvmStatic
        var instance: ConsoleOverlayPanel? = null
        var shader = 0
        internal var fontSmall = ConsoleFont.loadFont("graphics/fonts/jetbrains_mono_16.fnt")
        internal var fontLarge = ConsoleFont.loadFont("graphics/fonts/jetbrains_mono_32.fnt")
        var graphicsLib = Global.getSettings().modManager.isModEnabled("shaderLib")

        @JvmStatic
        var output = ""

        var COMMAND_HISTORY_PATH = "config/lw_console_command_history.json"
        var COMMAND_HISTORY_KEY = "commandHistory"
        private var lastCommandsJson: CommonDataJSONObject = JSONUtils.loadCommonJSON(COMMAND_HISTORY_PATH)

        @JvmStatic
        var lastCommands = ArrayList<String>()

        init {
            var lastCommandsArray = lastCommandsJson.optJSONArray(COMMAND_HISTORY_KEY) ?: JSONArray()

            for (i in 0 until lastCommandsArray.length()) {
                lastCommands.add(lastCommandsArray.getString(i))
            }
        }

    }

    var input = ""
    var lastInput = input
    var cursorIndex = input.length

    public var inputColor = Color(243, 245,  250)
    public var logColor = Color(188, 190, 196)
    public var compileErrorColor = Misc.getNegativeHighlightColor()
    //public var logColor = Color(178, 180, 186)
    public var grayColor = Color(188, 190, 196)

    private var inputDraw = fontSmall.createText("", inputColor, 16f)
    private var completionDraw = fontSmall.createText("", inputColor, 16f)
    private var compileDraw = fontSmall.createText("", compileErrorColor, 12f)
    private var syntaxDraw = fontSmall.createText("", grayColor, 16f)
    private var cursorDraw = fontSmall.createText("|", Color.white, 16f)
    private var arrowDraw = fontLarge.createText(">", Misc.getBasePlayerColor(), 24f)

    private var cursorBlinkInterval = IntervalUtil(0.5f, 0.5f)
    private var cursorBlink = true

    var wasPaused = false

    var completionSelectorIndex = 0
    var lastMatchesDisplayed = ArrayList<String>()

    var previousScroller = -1f

    var logElement: TooltipMakerAPI? = null

    var maxSavedCommands = 32
    var lastCommand = "" //Remember current command and always display when going back to index 0
    var lastCommandIndex = 0


    var requiresRecreation = false

    var placeHolderDialog: InteractionDialogAPI? = null

    var compileScope = CoroutineScope(Dispatchers.Default)
    var compileJob: Job? = null
    var compileError = ""

    init {

        //Open a dialog to prevent input from most other mods
        if (context.isInCampaign) {
            if (!Global.getSector().campaignUI.isShowingDialog) {
                Global.getSector().campaignUI.showInteractionDialog(PlaceholderDialog(), Global.getSector().playerFleet)
                placeHolderDialog = Global.getSector().campaignUI.currentInteractionDialog
            }
        }

        inputDraw.blendSrc = GL_ONE
        completionDraw.blendSrc = GL_ONE
        compileDraw.blendSrc = GL_ONE
        syntaxDraw.blendSrc = GL_ONE
        cursorDraw.blendSrc = GL_ONE
        arrowDraw.blendSrc = GL_ONE

        instance = this

        if (shader == 0 && graphicsLib) {
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
            wasPaused = Global.getCombatEngine().isPaused
            //Global.getCombatEngine().viewport.isExternalControl = true //Prevent moving screen with dragging
            Global.getCombatEngine().isPaused = true
        }

        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            wasPaused = Global.getSector().isPaused
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
        var widthOffset = 50f


        ///Background & Input Panel
        var backgroundPanel = parent.createCustomPanel(width, height, this)
        parent.addComponent(backgroundPanel)
        backgroundPanel.position.inTL(0f, 0f)

        //Limit output size
        var outputLimit = 3000
        var count = output.count { it == '\n' }

        while (count >= outputLimit) {
            var index = output.indexOf("\n")+1
            count -= 1
            if (index == -1) break
            output = output.substring(index, output.length)
            var test = ""
        }


        ///Logging Panel

        var toDraw = fontSmall.createText(output, logColor, 16f)
        toDraw.blendSrc = GL_ONE
        toDraw.maxWidth = width-widthOffset

        var logOffsetY = 95f
        var totalLogHeight = toDraw.height
        var logHeight = Math.min(height-logOffsetY, totalLogHeight)

        var logPanel = parent.createCustomPanel(width-widthOffset, logHeight, null)
        parent.addComponent(logPanel)

        logElement = logPanel.createUIElement(width-widthOffset, logHeight, true)

        var text = ConsoleTextElement(toDraw, logElement!!, toDraw.width, toDraw.height)

        logPanel.addUIElement(logElement)
        logPanel.position.inTL(widthOffset/2, height - logHeight - logOffsetY +10 )

        logElement!!.position.inTL(0f, 0f)

        //Reset scroller to top, restore previous scroller if input didnt change
        if (previousScroller == -1f) {
            logElement!!.externalScroller.yOffset = totalLogHeight-logHeight
        } else {
            logElement!!.externalScroller.yOffset = previousScroller
        }



        ///Text Input Panel
        var inputPanel = parent.createCustomPanel(width, height, null)
        parent.addComponent(inputPanel)
        inputPanel.position.inTL(0f, 0f)

        var inputElement = inputPanel.createUIElement(width, height, false)
        inputPanel.addUIElement(inputElement)
        inputElement.position.inTL(0f, 0f)


        createTextfield(inputPanel, inputElement, widthOffset)



    }

    fun createTextfield(panel: UIPanelAPI, element: TooltipMakerAPI, widthOffset: Float) {

        var width = parent.position.width
        var height = parent.position.height

        var innerSizeReduction = 30f

        //Ensure that cursor is clamped in to the input size
        cursorIndex = MathUtils.clamp(cursorIndex, 0, input.length)

        var isRuncode = input.lowercase().trimStart().startsWith("runcode ")
        if (!isRuncode) {
            compileError = ""
        }
        compileDraw.text = compileError
        compileDraw.maxWidth = width-widthOffset-innerSizeReduction
        compileDraw.triggerRebuildIfNeeded()

        var wordPartBeforeCursor = getWordBeforeCursor()
        var wordAtCursor = getFullWordAtCursor()
        var indexOfWord = getSubstringBeforeCursor().lastIndexOf(wordPartBeforeCursor) //Find out where to place the autocomplete panel
        var metNewLine = false
        var metWordStart = false

        val scaleFactor = inputDraw.fontSize / fontSmall.baseHeight
        var maxWidth = width - widthOffset - innerSizeReduction - 15
        var sizeSoFar = 0f
        var reconstruction = ""
        var offsetCorrection = 0
        var reconIndex = 0
        var widthUntilWordStart = -1f
        var lastChar: ConsoleFont.LazyChar? = null

        for (char in input) {
            reconstruction += char

            if (char == '\n') {
                sizeSoFar = 0f
                metNewLine = true
                continue
            }

            if (reconIndex >= indexOfWord) {
                metWordStart = true
            }

            if (widthUntilWordStart == -1f && metWordStart && !metNewLine) {
                widthUntilWordStart = sizeSoFar;
            }

            var ch = fontSmall.getChar(char)
            val kerning = if (lastChar != null) ch.getKerning(lastChar.id).toFloat() else 0f
            val charWidth = (kerning + ch.advance) * scaleFactor
            sizeSoFar += charWidth
            lastChar = ch
            //sizeSoFar += (inputDraw.fontSize-1) * scaleFactor

            if (sizeSoFar >= maxWidth) {
                reconstruction += "\n"

                //Needed to display the cursor correctly within the reconstructed string display
                if (reconIndex < cursorIndex) {
                    offsetCorrection += 1
                }

                sizeSoFar = 0f;

            }
            reconIndex++
        }

        inputDraw.text = reconstruction + " "
        inputDraw.baseColor = inputColor
        if (input.isEmpty()) {
            inputDraw.baseColor = Misc.getGrayColor()
            inputDraw.text = CommonStrings.INPUT_QUERY
        } else if (input.isBlank()) {
            inputDraw.text = " ".repeat(300) + "." //Small hack because LazyFont does not update paragraphs that are blank
        }
        //inputDraw.maxWidth = width-widthOffset-innerSizeReduction
        inputDraw.triggerRebuildIfNeeded()
        var textWidth = inputDraw.width


        var textHeight = Math.max(inputDraw.height, inputDraw.fontSize) //Height is 0 if theres no characters yet
        //Another dumb hack
        if (reconstruction.isBlank()) {
            textHeight = inputDraw.fontSize * (1+reconstruction.count { it == '\n' })
        }

        var matchedString = ""
        var p = Pattern.compile("[\\s\\S&&[^\\n]]+")
        for (c in reconstruction) {
            if (!p.matcher(c.toString()).matches()) {
                matchedString += c
            } else {
                matchedString += " "
            }
        }

        var cursorText = matchedString.substring(0, cursorIndex+offsetCorrection)

        //cursorDraw.text = " ".repeat(cursorIndex) + "|"
        cursorDraw.text = cursorText + "|"
        cursorDraw.baseColor = Misc.getBasePlayerColor()
        //cursorDraw.maxWidth = inputDraw.maxWidth
        cursorDraw.triggerRebuildIfNeeded()

        var syntax = getCurrentSyntax()
        syntaxDraw.text = cursorText + syntax
        syntaxDraw.baseColor = Color(188, 190, 196)
        //syntaxDraw.maxWidth = inputDraw.maxWidth
        syntaxDraw.triggerRebuildIfNeeded()

        var textfield = ConsoleTextfield(element, width-widthOffset, 30f+textHeight+compileDraw.height)
        textfield.position.inTL(width/2-textfield.width/2, height-textfield.height-30)

        textfield.render {
            arrowDraw.draw(textfield.x+10, textfield.y+textfield.height-10)
            inputDraw.draw(textfield.x+innerSizeReduction, textfield.y+textfield.height-15)

            if (cursorBlink) {
                cursorDraw.draw(textfield.x+innerSizeReduction-cursorDraw.fontSize/4+1, textfield.y+textfield.height-13)
                cursorDraw.draw(textfield.x+innerSizeReduction-cursorDraw.fontSize/4+1, textfield.y+textfield.height-17)
            }

            var word = getWordsWithIndex().lastOrNull()
            if (word != null && syntax.isNotBlank() && word.first+word.second.length == cursorIndex-1) {
                syntaxDraw.draw(textfield.x+innerSizeReduction, textfield.y+textfield.height-15)
            }

            if (compileError.isNotBlank()) {
                compileDraw.draw(textfield.x+innerSizeReduction, textfield.y+textfield.height-8-compileDraw.fontSize-inputDraw.height)
            }
        }

        /*var label2 = textContainer.innerElement.addPara(content, 0f)
        label2.position.inTL(0f, 0f)*/

        //println("Width: "+label2.computeTextWidth(label2.text))

        var matches = filterForMatches()
        lastMatchesDisplayed.clear()



        if (canShowSuggestions() && widthUntilWordStart != -1f) {
            addSuggestionWidget(matches, element, textfield.x+innerSizeReduction+widthUntilWordStart, height-textfield.height-30)
            lastMatchesDisplayed.addAll(matches)
        }


        //println(getWords())
    }

    fun canShowSuggestions() : Boolean {
        var matches = filterForMatches()
        var wordPartBeforeCursor = getWordBeforeCursor()

        var currentWords = getWordsWithIndex()
        var isToFarAway = true
        if (currentWords.size <= 1) isToFarAway = false //Ignore this bool for the command
        else {
            var last = currentWords.get(currentWords.size-1)
            var secondToLast = currentWords.get(currentWords.size-2)

            //If theres two spaces inbetween inputs its invalid for completion and wont show up.
            var diff = Math.abs(secondToLast.first + secondToLast.second.length - last.first)
            if (diff <= last.second.length+1) isToFarAway = false
        }

        return !isToFarAway && matches.isNotEmpty() && wordPartBeforeCursor.isNotBlank()/* && widthUntilWordStart != -1f*/
    }

    var maxMatches = 20

    fun filterForMatches() : List<String> {
        var word = getFullWordAtCursor()
        var matches: MutableList<String> = ArrayList<String>()

        var words = getWordsWithIndex()
        var last = words.lastOrNull()
        if (last == null || last.second.isBlank()) {
            return matches
        }

        //Select command or argument suggestions
        var completions = ArrayList<String>()
        if (words.size == 1) {
            var commands = CommandStore.getLoadedCommands()
            completions.addAll(commands)
        } else {
            var stored = getCommand()
            if (stored != null) {
                var command = stored.commandClass.newInstance()
                if (command is BaseCommandWithSuggestion) {
                    var withoutCommand = words.filter { words.first() != it }
                    var previous = withoutCommand.filter { withoutCommand.last() != it }.map { it.second }

                    completions.addAll(command.getSuggestions(withoutCommand.size-1, previous, context))
                }
            }
        }

        var count = 0
        for (completion in completions) {
            //if (!completion.lowercase().startsWith(word.lowercase())) continue
            if (!completion.lowercase().contains(word.lowercase())) continue
            matches.add(completion)
        }

        //matches = matches.sortedBy { it == word }.toMutableList()

        //Exact match / Alphabetical / Starts with
        matches = matches.sortedWith(compareBy<String> { it == word }.thenBy { it.lowercase().startsWith(word.lowercase()) }.thenByDescending { it }).toMutableList()

        while (matches.size > maxMatches) {
            matches.removeFirst()
        }

        return matches
    }

    //List of words in entire input.
    fun getWordsWithIndex() : List<Pair<Int, String>> {
        var list = ArrayList<Pair<Int, String>>()
        //var end = nextWordIndex()
        var end = input.length
        var sub = input.substring(0, end)

        var current = ""
        var index = 0
        var startIndex = 0
        for (char in sub) {
            index++
            if (char != ' ') {
                current += char
            } else {
                if (current.isNotBlank()) {
                    list.add(Pair(startIndex, current))
                    current = ""
                }
                startIndex = index
            }
        }

        //Required for end of sentence
        if (current.isNotBlank()) {
            list.add(Pair(index, current))
        }

        return list
    }

    fun getCommand() : StoredCommand? {
        var command: StoredCommand? = null
        var words = getWordsWithIndex()
        if (words.isNotEmpty()) {
            var word = words.get(0)
            command = CommandStore.retrieveCommand(word.second)
        }

       return command
    }

    fun getCurrentSyntax() : String {
        var syntax = ""
        var stored = getCommand()

        //Do not syntax any commands with the "no arguments" syntax
        if (stored?.syntax?.contains("(no arguments)") == true) {
            return ""
        }

        var split = stored?.syntax?.split(" ") ?: ArrayList<String>()
        var words = getWordsWithIndex()

        if (split.isNotEmpty() && words.isNotEmpty()) {
            var pick = split.getOrNull(words.size) //Grab next word
            if (pick != null) {
                syntax = pick
            }
        }



        return syntax
    }

    fun addSuggestionWidget(matches: List<String>, element: TooltipMakerAPI, locationX: Float, locationY: Float) {
        var bWidth = 200f
        var sbHeightOffset = 2
        var sbHeight = completionDraw.fontSize + sbHeightOffset

        var background = BaseConsoleElement(element, bWidth, 160f).apply {
            enableTransparency = true
            renderBorder = false
            backgroundColor = Color(15, 15, 20)
            backgroundAlpha = 0.95f // 0.5
        }

        var sbackground = BaseConsoleElement(element, bWidth, sbHeight).apply {
            enableTransparency = true
            renderBorder = false
            backgroundColor = Color(40, 40, 45)
            backgroundAlpha = 0.5f
        }

        var textRender = BaseConsoleElement(element, bWidth, 160f).apply {
            enableTransparency = true
            renderBorder = false
            backgroundColor = Color(20, 20, 25)
            backgroundAlpha = 0.9f // 0.5
        }

        completionSelectorIndex = MathUtils.clamp(completionSelectorIndex, 0, matches.size-1)


        var ratio = completionDraw.fontSize / fontSmall.baseHeight

        var bef = getWordBeforeCursor()
        var aft = getWordAfterCursor()
        var word = getFullWordAtCursor()

        var matchColor = Misc.getBasePlayerColor()

        var total = completionDraw.fontSize/2
        var textHeight = element.position.height - locationY - 1 + 5
        var maxParaWidth = 0f
        for (match in matches) {
            var toAdd = completionDraw.fontSize
            total += toAdd
            var hHeight = textHeight+toAdd
            textHeight = hHeight


            var pWidth = match.length * completionDraw.fontSize * ratio
            if (pWidth > maxParaWidth) {
                maxParaWidth = pWidth
            }

            textRender.render {
                completionDraw.text = " "
                completionDraw.triggerRebuildIfNeeded()

                if (match.lowercase().startsWith(bef.lowercase())) {
                    var leftPart = match.substring(0, bef.length)
                    var rightPart = match.substring(bef.length, match.length)
                    completionDraw.append(leftPart, matchColor).append(rightPart, inputDraw.baseColor)
                }
                else if (match.lowercase().contains(word.lowercase())) {
                    var startIndex = match.lowercase().indexOf(word.lowercase())
                    var endIndex = startIndex + word.length

                    var begin = match.substring(0, startIndex)
                    var highlight = match.substring(startIndex, endIndex)
                    var end = match.substring(endIndex, match.length)

                    completionDraw.append(begin, inputDraw.baseColor).append(highlight, matchColor).append(end, inputDraw.baseColor)
                } else {
                    //Should not happen, but oh well
                    completionDraw.text = " " + match
                }

                //completionDraw.append(match, matchColor).append(" ", matchColor)

                completionDraw.draw(locationX-completionDraw.fontSize/2, hHeight)
            }

        }

        if (maxParaWidth > bWidth) {
            bWidth = maxParaWidth + 50;
        }

        background.position.setSize(bWidth, total)
        background.position.inTL(locationX - 10, locationY-background.height)

        sbackground.position.setSize(bWidth, sbHeight)
        sbackground.position.inTL(locationX - 10, locationY-background.height+inputDraw.fontSize*completionSelectorIndex+sbHeightOffset)

    }

  /*  fun findMatchStart(match: String) : Int {

    }*/

    fun getWordBeforeIndex() : Int {
        var before = getSubstringBeforeCursor()
        var emptyLeft = before.lastIndexOf(" ")+1
        return emptyLeft
    }

    fun getWordBeforeCursor() : String {
        var before = getSubstringBeforeCursor()
        var emptyLeft = getWordBeforeIndex()
        var partLeft = before.substring(MathUtils.clamp(emptyLeft,0, cursorIndex), cursorIndex)

        return partLeft
    }

    fun getWordAfterIndex() : Int {
        var after = getSubstringAfterCursor()
        var emptyRight = after.indexOf(" ")
        if (emptyRight == -1) emptyRight = after.length
        return emptyRight
    }

    fun getWordAfterCursor() : String {
        var after = getSubstringAfterCursor()
        var emptyRight = getWordAfterIndex()
        var partRight = after.substring(0, MathUtils.clamp(emptyRight, 0, after.length))

        return partRight
    }

    fun getFullWordAtCursor() : String {
        var partLeft = getWordBeforeCursor()
        var partRight = getWordAfterCursor()
        return partLeft+partRight
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        previousScroller = logElement?.externalScroller?.yOffset ?: -1f

        cursorBlinkInterval.advance(amount)
        if (cursorBlinkInterval.intervalElapsed()) {
            cursorBlink =! cursorBlink
        }

        //Required to hide the hull/flux widget, as it exists outside of the normal UI tree
        var state = AppDriver.getInstance().currentState
        if (Global.getCurrentState() == GameState.COMBAT) {
            var reticle = ReflectionUtils.get("targetReticleRenderer", state)
            if (reticle != null) {
                var list = ReflectionUtils.get(null, reticle!!,  List::class.java) as MutableList<*>
                list.clear()
            }
        }

        if (requiresRecreation) {
            requiresRecreation = false
            recreatePanel()
        }
    }



    //Prevent input from going towards other windows
    override fun processInput(events: MutableList<InputEventAPI>) {

        var updatedCursor = false
        var previousSelector = completionSelectorIndex
        var previousOutput = output

        for (event in events) {

            cursorIndex = MathUtils.clamp(cursorIndex, 0, input.length)

            if (event.isConsumed) continue
            //if (!event.isKeyDownEvent) continue
            if (event.isKeyboardEvent && (event.isKeyDownEvent || event.isRepeat))
            {

                if (event.eventValue == Keyboard.KEY_ESCAPE) {
                    break
                }

                if (event.eventValue == Keyboard.KEY_LEFT) {
                    if (event.isCtrlDown) {
                        cursorIndex = lastWordIndex();
                    } else {
                        cursorIndex -= 1;
                    }
                    updatedCursor = true
                    event.consume()
                    break
                } else if (event.eventValue == Keyboard.KEY_RIGHT) {
                    if (event.isCtrlDown) {
                        cursorIndex = nextWordIndex();
                    } else {
                        cursorIndex += 1;
                    }

                    updatedCursor = true
                    event.consume()
                    break
                }

                var previousLastCommandIndex = lastCommandIndex
                if (event.eventValue == Keyboard.KEY_UP) {
                    if (!canShowSuggestions()) {
                        lastCommandIndex++
                        lastCommandIndex = MathUtils.clamp(lastCommandIndex,0, lastCommands.size)

                        if (previousLastCommandIndex != lastCommandIndex && lastCommandIndex != 0) {
                            input = lastCommands.get(lastCommandIndex-1)

                            //To prevent auto complete from starting while switching through
                            if (input.lastOrNull() != ' ') {
                                input += " "
                            }

                            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 0.9f)
                            cursorIndex = input.length
                        }
                    } else {
                        completionSelectorIndex -= 1;
                    }

                    event.consume()
                    break
                } else if (event.eventValue == Keyboard.KEY_DOWN) {

                    if (!canShowSuggestions()) {
                        lastCommandIndex--
                        lastCommandIndex = MathUtils.clamp(lastCommandIndex,0, lastCommands.size)

                        if (previousLastCommandIndex != lastCommandIndex) {
                            if (lastCommandIndex == 0) {
                                input = lastCommand
                            } else {
                                input = lastCommands.get(lastCommandIndex-1)
                            }

                            //To prevent auto complete from starting while switching through
                            if (input.lastOrNull() != ' ' && lastCommandIndex != 0) {
                                input += " "
                            }

                            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 0.9f)
                            cursorIndex = input.length
                        }
                    } else {
                        completionSelectorIndex += 1;
                    }

                    event.consume()
                    break
                }

                if (event.eventValue == Keyboard.KEY_TAB /*|| event.eventValue == Keyboard.KEY_RETURN*/)
                {
                    if (lastMatchesDisplayed.isNotEmpty()) {
                        var previous = input

                        completionSelectorIndex = MathUtils.clamp(completionSelectorIndex, 0, lastMatchesDisplayed.size-1)
                        var completion = lastMatchesDisplayed.get(lastMatchesDisplayed.size-completionSelectorIndex-1)

                        /*var current = getFullWordAtCursor()
                        input = input.replace(current, "$completion ") //Add space after completion.*/

                        var left = getWordBeforeIndex()
                        var right = getWordAfterIndex()
                        input = getSubstringBeforeCursor().substring(0, left) + "$completion " + getSubstringAfterCursor().substring(right)

                        cursorIndex += input.length-previous.length
                        Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 0.9f)

                    }

                    event.consume()
                    break
                }

                if (event.eventValue == Keyboard.KEY_V && event.isCtrlDown)
                {

                    try {
                        //var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor) as String
                        var clipboard = Sys.getClipboard()

                        var before = getSubstringBeforeCursor()
                        var after = getSubstringAfterCursor()
                        var sizeBefore = input.length
                        input = before
                        for (char in clipboard)
                        {
                            //appendCharIfPossible(char)
                            input += char
                        }
                        input += after
                        cursorIndex += input.length-sizeBefore
                    } catch (e: Throwable) {

                    }


                    event.consume()
                    break
                }

                if (event.eventValue == Keyboard.KEY_BACK)
                {
                    var before = getSubstringBeforeCursor()
                    var after = getSubstringAfterCursor()

                    if (before.isEmpty())
                    {
                        event.consume()
                        break
                    }
                    else if (event.isShiftDown)
                    {
                        /*var priorSize = input.length
                        input = after
                        cursorIndex -= priorSize-input.length*/
                        input = ""
                        cursorIndex = 0
                        event.consume()
                        break
                    }
                    else if (event.isCtrlDown)
                    {
                        deleteLastWord()
                        event.consume()
                        break
                    }
                    else
                    {
                        //playSound("ui_typer_type")
                        //input = (input.substring(0, input.length - 1))

                        input = (before.substring(0, before.length - 1)) + after
                        event.consume()
                        cursorIndex -= 1
                        break
                    }
                }

                /*if (event.isCtrlDown || event.isAltDown)
                {
                    event.consume()
                    break
                }*/

                if (event.eventValue == Keyboard.KEY_RETURN) {


                    if (event.isShiftDown) {
                        input = getSubstringBeforeCursor() + "\n" + getSubstringAfterCursor()
                        cursorIndex += 1
                    } else {

                        //Input needs to be reset before execution so that the history command can modify it
                        var commandInput = input
                        input = ""
                        cursorIndex = 0

                        when {
                            // TODO: Ensure newlines are supported everywhere (currently replaced with spaces where not supported)
                            commandInput.startsWith("runcode ", true) -> Console.parseInput(commandInput, context)
                            else -> Console.parseInput(commandInput.replace('\n', ' '), context)
                        }

                        //Only save if the command entered isnt equal to the one before, and those that arent blank
                        lastCommandIndex = 0
                        if (lastCommands.getOrNull(0) != commandInput && commandInput.isNotBlank() && !commandInput.lowercase().startsWith("history")) {
                            lastCommands.add(0, commandInput)
                            if (lastCommands.size > maxSavedCommands) {
                                lastCommands.removeLast()
                            }
                            lastCommandsJson.put(COMMAND_HISTORY_KEY, lastCommands)
                            lastCommandsJson.save()
                            lastCommand = ""
                        }


                        break
                    }


                    event.consume()

                    break
                }



                var char = event.eventChar

                if (isValidChar(char)) {
                    //input += char
                    input = getSubstringBeforeCursor() + char + getSubstringAfterCursor()
                    cursorIndex += 1
                    event.consume()
                    break
                }

                //appendCharIfPossible(char)

            }
        }

        if (input != lastInput) {
            completionSelectorIndex = 0
        }

        if (output != previousOutput) {
            previousScroller = -1f
        }

        if (completionSelectorIndex != previousSelector) {
            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 0.9f)
        }

        if (input != lastInput || updatedCursor || completionSelectorIndex != previousSelector || output != previousOutput) {

            //Update the saved, non-entered command when not being on the first index
            if (lastCommandIndex == 0)  {
                lastCommand = input
            }

            //cancel previous compile job as it will no longer provide the correct message
            if (compileJob != null) {
                if (compileJob?.isCancelled == false) {
                    compileJob?.cancel()
                }
            }

            var isRuncode = input.lowercase().trimStart().startsWith("runcode ")
            if (isRuncode) {
                //remove the "runcode" part
                var toCompile = input.substring(input.indexOf(" "), input.length).trim()
                if (!toCompile.endsWith(";")) {
                    toCompile += ";"
                }
                if (toCompile.isNotBlank()) {

                    //Run compilation on another coroutine to prevent lag
                    compileJob = compileScope.launch {
                        delay(100) //Short delay to cancel in case other input arrived and to make the error display less flashing
                        if (isActive) {
                            try {
                                RunCode.eval.cook(toCompile)
                                compileError = ""
                            } catch (e: CompileException) {
                                compileError = e.message ?: ""
                            }
                            requiresRecreation = true
                        }
                    }
                }
            }

            cursorBlink = true
            cursorBlinkInterval.elapsed = 0f

            lastInput = input
            recreatePanel()
        }

        //Consume unconsumed keys to prevent interacting with other elements
        for (event in events)
        {
            if (event.isConsumed) continue
            if (event.isKeyDownEvent && event.eventValue == Keyboard.KEY_ESCAPE)
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

    private fun getSubstringBeforeCursor() = input.substring(0, cursorIndex)

    private fun getSubstringAfterCursor() = input.substring(cursorIndex)

    private fun deleteLastWord()
    {

        var priorSize = input.length

        var before = getSubstringBeforeCursor()
        var after = getSubstringAfterCursor()

        var last = before.lastIndexOf(" ")
        if (last == before.length - 1 && last > 0)
        {
            last = before.substring(0, last).lastIndexOf(" ")
        }

        if (last == -1)
        {
            input = after
            cursorIndex -= priorSize-input.length
        }
        else
        {
            input = input.substring(0, last + 1) + after
            cursorIndex -= priorSize-input.length
        }
    }

    private fun lastWordIndex() : Int {
        var before = getSubstringBeforeCursor()

        var last = before.lastIndexOf(" ")
        if (last == before.length - 1 && last > 0)
        {
            last = before.substring(0, last).lastIndexOf(" ")
        }

        return MathUtils.clamp(last, 0, input.length)
    }

    private fun nextWordIndex() : Int {
        var after = getSubstringAfterCursor()
        var first = after.indexOf(" ")
        if (first == -1) {
            return input.length
        }


        first += getSubstringBeforeCursor().length+1

      /*  if (cursorIndex == first) {
            return cursorIndex+1
        }*/

        return MathUtils.clamp(first, 0, input.length)
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

        if (char != null) {
            return fontSmall.getChar(char) != fontSmall.fallbackChar
        }

        return true
    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

    }

    override fun renderBelow(alphaMult: Float) {

        //Screen texture can be unloaded if graphicslib shaders are disabled, causing a blackscreen
        if (graphicsLib && ShaderLib.getScreenTexture() != 0) {
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
        //No GraphicsLib or Disabled Shaders
        else {
            var color = Color.black
            var alpha = 0.9f

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)


            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            GL11.glColor4f(color.red / 255f,
                color.green / 255f,
                color.blue / 255f,
                color.alpha / 255f * (alphaMult * alpha))

            GL11.glRectf(0f, 0f , Global.getSettings().screenWidth, Global.getSettings().screenHeight)

            GL11.glPopMatrix()
        }
    }

    public fun close() {

        compileScope.cancel()

        var state = AppDriver.getInstance().currentState

        if (Global.getCurrentState() == GameState.COMBAT) {
            if (!wasPaused) Global.getCombatEngine().isPaused = false
            //Global.getCombatEngine().viewport.isExternalControl = false
            //ReflectionUtils.set("hideHud", state, false)
        }

        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            if (!wasPaused) Global.getSector().isPaused = false;
        }

        placeHolderDialog?.dismiss()
        placeHolderDialog = null

        instance = null
        parent.getParent()?.removeComponent(parent)
    }

    class PlaceholderDialog() : InteractionDialogPlugin {

        var dialog: InteractionDialogAPI? = null

        override fun init(dialog: InteractionDialogAPI?) {
            this.dialog = dialog

            dialog!!.promptText = "Test"
            /*if (dialog is UIPanelAPI) {
                dialog.opacity = 0f
               //var fader =ReflectionUtils.invoke("getFader", dialog) as Fader
            }*/
        }

        override fun optionSelected(optionText: String?, optionData: Any?) {

        }

        override fun optionMousedOver(optionText: String?, optionData: Any?) {

        }

        override fun advance(amount: Float) {
            //ReflectionUtils.invoke("setOpacity", dialog!!, 0f)
            var fader = ReflectionUtils.invoke("getFader", dialog!!.visualPanel) as Fader
            fader.brightness = 0f
        }

        override fun backFromEngagement(battleResult: EngagementResultAPI?) {

        }

        override fun getContext(): Any? {
            return null
        }

        override fun getMemoryMap(): MutableMap<String, MemoryAPI> {
            return hashMapOf()
        }

    }

}