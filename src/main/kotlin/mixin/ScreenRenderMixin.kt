package ru.mrdire.chatselect.mixin

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import ru.mrdire.chatselect.ChatTextSelectState
import ru.mrdire.chatselect.ChatTextSelection

import ru.mrdire.chatselect.ChatTextSelectDragState
import net.minecraft.client.gui.components.ChatComponent

@Mixin(Screen::class)
abstract class ScreenRenderMixin {

    @Unique
    private fun chatTextSelect_currentScreen(client: Minecraft): Any? {
        val method = client.javaClass.methods.firstOrNull {
            it.parameterCount == 0 &&
                    (
                            it.name == "screen" ||
                                    it.name == "getScreen"
                            )
        }

        if (method != null) {
            return method.invoke(client)
        }

        val field = client.javaClass.declaredFields.firstOrNull {
            it.name == "screen"
        } ?: return null

        field.isAccessible = true
        return field.get(client)
    }

//    @Unique
//    private fun chatTextSelect_chatComponent(client: Minecraft): Any {
//        val gui = client.gui
//
//        val method = gui.javaClass.methods.firstOrNull {
//            it.parameterCount == 0 &&
//                    ChatComponent::class.java.isAssignableFrom(it.returnType)
//        }
//
//        if (method != null) {
//            return method.invoke(gui)
//        }
//
//        val field = gui.javaClass.declaredFields.firstOrNull {
//            ChatComponent::class.java.isAssignableFrom(it.type)
//        } ?: error("ChatTextSelect: cannot find ChatComponent in Gui")
//
//        field.isAccessible = true
//        return field.get(gui)
//    }

    @Inject(method = ["extractRenderState"], at = [At("TAIL")])
    private fun onExtractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        tickDelta: Float,
        ci: CallbackInfo
    ) {
        val client = Minecraft.getInstance()

        if (!ChatTextSelectState.enabled) return
        if ((this as Any) !is ChatScreen) return

        val chatAccessor = client.gui.hud.getChat() as ChatHudAccessor

        val lines = ChatTextSelection.visibleLinesToPlainText(
            chatAccessor.chatTextSelect_getTrimmedMessages()
        )

        val chatScale = chatAccessor.chatTextSelect_getScale()
        val lineHeight = chatAccessor.chatTextSelect_getLineHeight()
        val scrollOffset = chatAccessor.chatTextSelect_getChatScrollbarPos()

        val chatLeft = 1
        val chatBottom = client.window.guiScaledHeight - 40

        val windowHandle = chatTextSelect_windowHandle(client)

        val leftMouseDown =
            windowHandle != 0L &&
                    GLFW.glfwGetMouseButton(
                        windowHandle,
                        GLFW.GLFW_MOUSE_BUTTON_LEFT
                    ) == GLFW.GLFW_PRESS

        if (leftMouseDown) {
            val localX = (mouseX - chatLeft) / chatScale
            val localYFromBottom = (chatBottom - mouseY) / chatScale

            val visualLineIndex = ChatTextSelection.mouseToLine(
                localYFromBottom,
                lineHeight,
                lines.size
            )

            if (visualLineIndex != null) {
                val lineIndex = visualLineIndex + scrollOffset

                if (lineIndex in lines.indices) {
                    val line = lines[lineIndex].text

                    val charIndex = ChatTextSelection.mouseToChar(
                        client.font,
                        line,
                        localX
                    )

                    if (ChatTextSelectDragState.hasMovedEnough(
                            mouseX.toDouble(),
                            mouseY.toDouble()
                        )
                    ) {
                        ChatTextSelection.dragIfMoved(
                            lines,
                            lineIndex,
                            charIndex
                        )
                    }
                }
            }
        } else {
            ChatTextSelectDragState.stop()
            ChatTextSelection.finish()
        }

        if (!ChatTextSelection.hasSelection()) return

        ChatTextSelection.renderSelection(
            graphics,
            client.font,
            lines,
            chatLeft,
            chatBottom,
            lineHeight,
            chatScale,
            scrollOffset
        )
    }

    @Unique
    private fun chatTextSelect_windowHandle(client: Minecraft): Long {
        val window = client.window

        val method = window.javaClass.methods.firstOrNull {
            it.parameterCount == 0 &&
                    it.returnType == Long::class.javaPrimitiveType &&
                    (
                            it.name == "handle" ||
                                    it.name == "getWindow" ||
                                    it.name == "getWindowHandle" ||
                                    it.name == "getHandle"
                            )
        } ?: return 0L

        val value = method.invoke(window)

        return if (value is Number) {
            value.toLong()
        } else {
            0L
        }
    }
}