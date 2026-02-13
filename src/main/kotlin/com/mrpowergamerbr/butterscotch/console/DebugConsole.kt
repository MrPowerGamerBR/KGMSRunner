package com.mrpowergamerbr.butterscotch.console

import com.mrpowergamerbr.butterscotch.graphics.Renderer
import com.mrpowergamerbr.butterscotch.runtime.GameRunner
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

class DebugConsole(
    internal val runner: GameRunner,
    private val windowWidth: Int,
    private val windowHeight: Int
) {
    var isOpen = false
        private set
    // Used to avoid the toggle key being written in the input buffer while opening
    var toggledConsoleOnThisFrame = false

    val inputBuffer = StringBuilder()
    private val outputLines = mutableListOf<String>()
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    private var consoleTexture = -1
    private var dirty = true

    private val consoleHeight = windowHeight / 2  // 240px for 480px window
    private val font = Font(Font.MONOSPACED, Font.PLAIN, 14)

    internal val commands = listOf<DebugConsoleCommand>(
        HelpCommand(this),
        ClearCommand(this),
        RoomGotoCommand(this),
        RoomInfoCommand(this),
        InstancesCommand(this),
        FluffyBoiCommand(this),
    )

    fun toggle() {
        isOpen = !isOpen
        dirty = true
        toggledConsoleOnThisFrame = true
    }

    fun onChar(codepoint: Int) {
        val ch = codepoint.toChar()
        inputBuffer.append(ch)
        dirty = true
    }

    fun onKey(key: Int): Boolean {
        when (key) {
            GLFW.GLFW_KEY_ENTER -> {
                val command = inputBuffer.toString().trim()
                if (command.isNotEmpty()) {
                    outputLines.add("> $command")
                    commandHistory.add(command)
                    historyIndex = -1
                    executeCommand(command)
                    inputBuffer.clear()
                }
                dirty = true
                return true
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (inputBuffer.isNotEmpty()) {
                    inputBuffer.deleteCharAt(inputBuffer.length - 1)
                    dirty = true
                }
                return true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                isOpen = false
                dirty = true
                return true
            }
            GLFW.GLFW_KEY_UP -> {
                if (commandHistory.isNotEmpty()) {
                    if (historyIndex == -1) {
                        historyIndex = commandHistory.size - 1
                    } else if (historyIndex > 0) {
                        historyIndex--
                    }
                    inputBuffer.clear()
                    inputBuffer.append(commandHistory[historyIndex])
                    dirty = true
                }
                return true
            }
            GLFW.GLFW_KEY_DOWN -> {
                if (historyIndex >= 0) {
                    historyIndex++
                    if (historyIndex >= commandHistory.size) {
                        historyIndex = -1
                        inputBuffer.clear()
                    } else {
                        inputBuffer.clear()
                        inputBuffer.append(commandHistory[historyIndex])
                    }
                    dirty = true
                }
                return true
            }
        }
        return false
    }

    fun addOutput(line: String) {
        outputLines.add(line)
        // Cap scrollback
        while (outputLines.size > 50) {
            outputLines.removeAt(0)
        }
        dirty = true
    }

    fun clearOutput() {
        outputLines.clear()
        dirty = true
    }

    private fun executeCommand(command: String) {
        val parts = command.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        val matchedCommand = commands.firstOrNull { it.name == cmd }

        if (matchedCommand == null) {
            addOutput("Unknown command: $cmd (type 'help' for commands)")
            return
        }

        matchedCommand.execute(args)
    }

    fun render(renderer: Renderer) {
        if (!isOpen) return

        this.toggledConsoleOnThisFrame = false

        if (dirty || consoleTexture == -1) {
            renderToTexture()
            dirty = false
        }

        if (consoleTexture >= 0) {
            renderer.drawRawTexture(consoleTexture, 0.0, 0.0, windowWidth.toDouble(), consoleHeight.toDouble(), windowWidth, windowHeight)
        }
    }

    private fun renderToTexture() {
        val img = BufferedImage(windowWidth, consoleHeight, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Semi-transparent black background
        g.color = Color(0, 0, 0, 217) // ~85% opacity
        g.fillRect(0, 0, windowWidth, consoleHeight)

        // Bottom separator line
        g.color = Color(128, 128, 128)
        g.drawLine(0, consoleHeight - 1, windowWidth, consoleHeight - 1)

        g.font = font
        val fm = g.fontMetrics
        val lineH = fm.height
        val padding = 4

        // Input line at the bottom (above separator)
        val inputY = consoleHeight - padding - fm.descent
        g.color = Color(255, 255, 255)
        val inputText = "> ${inputBuffer}_"
        g.drawString(inputText, padding, inputY)

        // Output lines above the input line, drawn bottom-up
        g.color = Color(200, 200, 200)
        val maxVisibleLines = (inputY - lineH - padding) / lineH
        val startIdx = maxOf(0, outputLines.size - maxVisibleLines)
        var y = inputY - lineH
        for (i in outputLines.size - 1 downTo startIdx) {
            if (y < padding) break
            g.drawString(outputLines[i], padding, y)
            y -= lineH
        }

        g.dispose()

        // Convert BufferedImage to RGBA ByteBuffer
        val pixels = (img.raster.dataBuffer as DataBufferInt).data
        val buffer = BufferUtils.createByteBuffer(windowWidth * consoleHeight * 4)
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte()) // R
            buffer.put(((pixel shr 8) and 0xFF).toByte())  // G
            buffer.put((pixel and 0xFF).toByte())           // B
            buffer.put(((pixel shr 24) and 0xFF).toByte()) // A
        }
        buffer.flip()

        // Upload to OpenGL texture
        if (consoleTexture == -1) {
            consoleTexture = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, consoleTexture)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, windowWidth, consoleHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, consoleTexture)
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, windowWidth, consoleHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
        }
    }

    fun dispose() {
        if (consoleTexture >= 0) {
            GL11.glDeleteTextures(consoleTexture)
            consoleTexture = -1
        }
    }
}