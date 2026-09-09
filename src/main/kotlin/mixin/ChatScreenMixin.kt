package ru.mrdire.chatselect.mixin

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import ru.mrdire.chatselect.ChatTextSelectState
import ru.mrdire.chatselect.ChatTextSelection

import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.gui.components.ChatComponent
import org.slf4j.LoggerFactory

import ru.mrdire.chatselect.ChatTextSelectDragState


@Mixin(ChatScreen::class)
abstract class ChatScreenMixin {

//    companion object {
//        private val LOGGER = LoggerFactory.getLogger("ChatTextSelect")
//    }

    @Unique
    private var chatTextSelect_mouseDown = false

    @Unique
    private var chatTextSelect_startMouseX = 0.0

    @Unique
    private var chatTextSelect_startMouseY = 0.0

    @Unique
    private val chatTextSelect_dragThreshold = 5.0

    @Unique
    private var chatTextSelect_lastClickTime = 0L

    @Unique
    private var chatTextSelect_lastClickX = 0.0

    @Unique
    private var chatTextSelect_lastClickY = 0.0

    @Unique
    private fun chatTextSelect_client(): Minecraft {
        return Minecraft.getInstance()
    }

    @Unique
    private fun chatTextSelect_accessor(): ChatHudAccessor =
        chatTextSelect_client().gui.hud.getChat() as ChatHudAccessor

    @Unique
    private fun chatTextSelect_chatComponent(): Any {
        val gui = chatTextSelect_client().gui

        val method = gui.javaClass.methods.firstOrNull {
            it.parameterCount == 0 &&
                    ChatComponent::class.java.isAssignableFrom(it.returnType)
        }

        if (method != null) {
            return method.invoke(gui)
        }

        val field = gui.javaClass.declaredFields.firstOrNull {
            ChatComponent::class.java.isAssignableFrom(it.type)
        } ?: error("ChatTextSelect: cannot find ChatComponent in Gui")

        field.isAccessible = true
        return field.get(gui)
    }

    @Unique
    private fun chatTextSelect_plainLines(): List<ChatTextSelection.VisibleLine> {
        return ChatTextSelection.visibleLinesToPlainText(
            chatTextSelect_accessor().chatTextSelect_getTrimmedMessages()
        )
    }

    @Unique
    private fun chatTextSelect_chatLeft(): Int {
        return 1
    }

    @Unique
    private fun chatTextSelect_chatBottom(): Int {
        val client = chatTextSelect_client()
        return client.window.guiScaledHeight - 40
    }

    @Unique
    private fun chatTextSelect_chatScale(): Double {
        return chatTextSelect_accessor().chatTextSelect_getScale()
    }

    @Unique
    private fun chatTextSelect_lineHeight(): Int {
        return chatTextSelect_accessor().chatTextSelect_getLineHeight()
    }

    @Unique
    private fun chatTextSelect_scrollOffset(): Int {
        return chatTextSelect_accessor().chatTextSelect_getChatScrollbarPos()
    }

    @Unique
    private fun chatTextSelect_isDoubleClick(mouseX: Double, mouseY: Double): Boolean {
        val now = System.currentTimeMillis()

        val dx = mouseX - chatTextSelect_lastClickX
        val dy = mouseY - chatTextSelect_lastClickY
        val distanceSq = dx * dx + dy * dy

        val result = now - chatTextSelect_lastClickTime <= 350L && distanceSq <= 25.0

        chatTextSelect_lastClickTime = now
        chatTextSelect_lastClickX = mouseX
        chatTextSelect_lastClickY = mouseY

        return result
    }

    @Unique
    private fun chatTextSelect_mouseEventDouble(
        event: Any,
        vararg names: String
    ): Double? {
        for (name in names) {
            val method = event.javaClass.methods.firstOrNull {
                it.name == name && it.parameterCount == 0
            }

            val value = method?.invoke(event)

            if (value is Number) {
                return value.toDouble()
            }
        }

        for (field in event.javaClass.declaredFields) {
            if (field.name in names) {
                field.isAccessible = true
                val value = field.get(event)

                if (value is Number) {
                    return value.toDouble()
                }
            }
        }

        return null
    }

    @Unique
    private fun chatTextSelect_mouseEventInt(
        event: Any,
        vararg names: String
    ): Int? {
        for (name in names) {
            val method = event.javaClass.methods.firstOrNull {
                it.name == name && it.parameterCount == 0
            }

            val value = method?.invoke(event)

            if (value is Number) {
                return value.toInt()
            }
        }

        for (field in event.javaClass.declaredFields) {
            if (field.name in names) {
                field.isAccessible = true
                val value = field.get(event)

                if (value is Number) {
                    return value.toInt()
                }
            }
        }

        return null
    }

    @Unique
    private fun chatTextSelect_keyEventInt(
        event: Any,
        vararg names: String
    ): Int? {
        for (name in names) {
            val method = event.javaClass.methods.firstOrNull {
                it.name == name && it.parameterCount == 0
            }

            val value = method?.invoke(event)

            if (value is Number) {
                return value.toInt()
            }
        }

        for (field in event.javaClass.declaredFields) {
            if (field.name in names) {
                field.isAccessible = true
                val value = field.get(event)

                if (value is Number) {
                    return value.toInt()
                }
            }
        }

        return null
    }

    @Unique
    private fun chatTextSelect_isClickableChatComponent(
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        val client = chatTextSelect_client()
        val chat = chatTextSelect_client().gui.hud.getChat()

        val possibleMethods = listOf(
            "getClickedComponentStyleAt",
            "getComponentStyleAt",
            "getTextStyleAt"
        )

        for (methodName in possibleMethods) {
            val method = chat.javaClass.methods.firstOrNull {
                it.name == methodName &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0] == Double::class.javaPrimitiveType &&
                        it.parameterTypes[1] == Double::class.javaPrimitiveType
            } ?: continue

            val style = method.invoke(chat, mouseX, mouseY) ?: continue

            val hasClickEvent = style.javaClass.methods.any {
                it.name == "getClickEvent" &&
                        it.parameterCount == 0
            }

            if (!hasClickEvent) continue

            val clickEvent = style.javaClass.methods
                .firstOrNull {
                    it.name == "getClickEvent" &&
                            it.parameterCount == 0
                }
                ?.invoke(style)

            if (clickEvent != null) {
                return true
            }
        }

        return false
    }

    @Inject(method = ["mouseClicked"], at = [At("HEAD")], cancellable = false)
    private fun onMouseClicked(
        event: MouseButtonEvent,
        doubled: Boolean,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (!ChatTextSelectState.enabled) return

        val mouseX = chatTextSelect_mouseEventDouble(
            event,
            "x",
            "mouseX",
            "getX",
            "getMouseX"
        ) ?: return

        val mouseY = chatTextSelect_mouseEventDouble(
            event,
            "y",
            "mouseY",
            "getY",
            "getMouseY"
        ) ?: return

        val button = chatTextSelect_mouseEventInt(
            event,
            "button",
            "key",
            "getButton",
            "buttonId"
        ) ?: return

//        LOGGER.info(
//            "ChatTextSelect mouseClicked: x={}, y={}, button={}, doubled={}",
//            mouseX,
//            mouseY,
//            button,
//            doubled
//        )

        println("ChatTextSelect mouseClicked: x=$mouseX, y=$mouseY, button=$button, doubled=$doubled")

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return

        if (chatTextSelect_isClickableChatComponent(mouseX, mouseY)) {
            ChatTextSelection.clear()
            println("ChatTextSelect ignored clickable chat component")
            return
        }

        val client = chatTextSelect_client()
        val lines = chatTextSelect_plainLines()
        val scale = chatTextSelect_chatScale()

        val scrollOffset = chatTextSelect_scrollOffset()

        val localX = (mouseX - chatTextSelect_chatLeft()) / scale
        val localYFromBottom = (chatTextSelect_chatBottom() - mouseY) / scale

        val visualLineIndex = ChatTextSelection.mouseToLine(
            localYFromBottom,
            chatTextSelect_lineHeight(),
            lines.size
        ) ?: return

        val lineIndex = visualLineIndex + scrollOffset
        if (lineIndex !in lines.indices) return

        val line = lines[lineIndex].text

        val charIndex = ChatTextSelection.mouseToChar(
            client.font,
            line,
            localX
        )

        if (doubled) {
            ChatTextSelectDragState.stop()

            ChatTextSelection.selectWord(
                lines,
                lineIndex,
                charIndex
            )

            println("ChatTextSelect selected word")
        } else {
            ChatTextSelectDragState.start(mouseX, mouseY)

            ChatTextSelection.prepare(
                lines,
                lineIndex,
                charIndex
            )

            println("ChatTextSelect selection prepared: lineIndex=$lineIndex, charIndex=$charIndex")
        }
    }

//    @Inject(method = ["mouseReleased"], at = [At("HEAD")], cancellable = false)
//    private fun onMouseReleased(
//        mouseX: Double,
//        mouseY: Double,
//        button: Int,
//        cir: CallbackInfoReturnable<Boolean>
//    ) {
//        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
//            chatTextSelect_mouseDown = false
//            ChatTextSelection.finish()
//        }
//    }

    @Inject(method = ["keyPressed"], at = [At("HEAD")], cancellable = true)
    private fun onKeyPressed(
        event: KeyEvent,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (!ChatTextSelectState.enabled) return

        val keyCode = chatTextSelect_keyEventInt(
            event,
            "key",
            "keyCode",
            "keycode",
            "getKey",
            "getKeyCode"
        ) ?: return

        val modifiers = chatTextSelect_keyEventInt(
            event,
            "modifiers",
            "mods",
            "getModifiers"
        ) ?: 0

        println("ChatTextSelect keyPressed: keyCode=$keyCode, modifiers=$modifiers")

        val isCopy = keyCode == GLFW.GLFW_KEY_C &&
                modifiers and GLFW.GLFW_MOD_CONTROL != 0

        if (isCopy) {
            println("ChatTextSelect Ctrl+C detected, hasSelection=${ChatTextSelection.hasSelection()}")

            if (ChatTextSelection.hasSelection()) {
                ChatTextSelection.copyToClipboard(chatTextSelect_client())
                println("ChatTextSelect copied selection")
                cir.returnValue = true
            }
        }
    }

//    @Inject(method = ["render"], at = [At("TAIL")])
//    private fun onRender(
//        graphics: Any,
//        mouseX: Int,
//        mouseY: Int,
//        deltaTicks: Float,
//        ci: CallbackInfo
//    ) {
//        if (!ChatTextSelectState.enabled) return
//
//        val client = chatTextSelect_client()
//        val lines = chatTextSelect_plainLines()
//        val scale = chatTextSelect_chatScale()
//
//        val scrollOffset = chatTextSelect_scrollOffset()
//
//        if (chatTextSelect_mouseDown) {
//            val localX = (mouseX - chatTextSelect_chatLeft()) / scale
//            val localYFromBottom = (chatTextSelect_chatBottom() - mouseY) / scale
//
//            val visualLineIndex = ChatTextSelection.mouseToLine(
//                localYFromBottom,
//                chatTextSelect_lineHeight(),
//                lines.size
//            )
//
//            val lineIndex = if (visualLineIndex != null) {
//                visualLineIndex + scrollOffset
//            } else {
//                null
//            }
//
//            if (lineIndex != null && lineIndex in lines.indices) {
//                val line = lines[lineIndex].text
//
//                val charIndex = ChatTextSelection.mouseToChar(
//                    client.font,
//                    line,
//                    localX
//                )
//
//                val movedX = mouseX - chatTextSelect_startMouseX
//                val movedY = mouseY - chatTextSelect_startMouseY
//                val movedDistanceSq = movedX * movedX + movedY * movedY
//
//                if (movedDistanceSq >= chatTextSelect_dragThreshold * chatTextSelect_dragThreshold) {
//                    ChatTextSelection.dragIfMoved(
//                        lines,
//                        lineIndex,
//                        charIndex
//                    )
//                }
//            }
//
//            chatTextSelect_mouseDown = false
//            ChatTextSelection.finish()
//        }
//
//        ChatTextSelection.renderSelection(
//            graphics,
//            client.font,
//            lines,
//            chatTextSelect_chatLeft(),
//            chatTextSelect_chatBottom(),
//            chatTextSelect_lineHeight(),
//            scale,
//            scrollOffset
//        )
//    }
}