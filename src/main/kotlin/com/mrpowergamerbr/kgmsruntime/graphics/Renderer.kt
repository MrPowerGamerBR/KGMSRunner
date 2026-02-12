package com.mrpowergamerbr.kgmsruntime.graphics

import com.mrpowergamerbr.kgmsruntime.data.GameData
import org.lwjgl.opengl.GL11.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class Renderer(val gameData: GameData) {
    // OpenGL textures for each TXTR page
    private val textures = IntArray(gameData.texturePages.size) { -1 }
    private val textureWidths = IntArray(gameData.texturePages.size)
    private val textureHeights = IntArray(gameData.texturePages.size)

    // Drawing state
    var drawColor: Int = 0xFFFFFF   // BGR format
    var drawAlpha: Double = 1.0
    var drawFont: Int = -1
    var drawHalign: Int = 0  // 0=left, 1=center, 2=right
    var drawValign: Int = 0  // 0=top, 1=middle, 2=bottom

    var currentView: Int = 0
    var framebufferScaleX: Float = 1.0f
    var framebufferScaleY: Float = 1.0f

    private var viewX = 0
    private var viewY = 0
    private var viewW = 640
    private var viewH = 480

    fun initialize() {
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    fun setView(vx: Int, vy: Int, vw: Int, vh: Int, px: Int, py: Int, pw: Int, ph: Int) {
        viewX = vx; viewY = vy; viewW = vw; viewH = vh

        glViewport(
            (px * framebufferScaleX).toInt(),
            (py * framebufferScaleY).toInt(),
            (pw * framebufferScaleX).toInt(),
            (ph * framebufferScaleY).toInt()
        )
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        glOrtho(vx.toDouble(), (vx + vw).toDouble(), (vy + vh).toDouble(), vy.toDouble(), -1.0, 1.0)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
    }

    fun clear(bgColor: Int) {
        // bgColor is in ABGR or BGR format
        val r = (bgColor and 0xFF) / 255f
        val g = ((bgColor shr 8) and 0xFF) / 255f
        val b = ((bgColor shr 16) and 0xFF) / 255f
        glClearColor(r, g, b, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
    }

    fun drawSprite(spriteIndex: Int, subImage: Int, x: Double, y: Double,
                   xscale: Double = 1.0, yscale: Double = 1.0, angle: Double = 0.0,
                   blend: Int = 0xFFFFFF, alpha: Double = 1.0) {
        if (spriteIndex < 0 || spriteIndex >= gameData.sprites.size) return
        val sprite = gameData.sprites[spriteIndex]
        val frameIdx = if (sprite.tpagIndices.isEmpty()) return else {
            subImage.mod(sprite.tpagIndices.size)
        }
        val tpagIdx = sprite.tpagIndices[frameIdx]
        if (tpagIdx < 0) return
        val tpag = gameData.texturePageItems[tpagIdx]

        val texId = ensureTexture(tpag.texturePageId)
        if (texId < 0) return

        val texW = textureWidths[tpag.texturePageId].toFloat()
        val texH = textureHeights[tpag.texturePageId].toFloat()
        if (texW == 0f || texH == 0f) return

        val u0 = tpag.sourceX / texW
        val v0 = tpag.sourceY / texH
        val u1 = (tpag.sourceX + tpag.sourceWidth) / texW
        val v1 = (tpag.sourceY + tpag.sourceHeight) / texH

        val ox = sprite.originX.toDouble()
        val oy = sprite.originY.toDouble()

        // Target position with offset
        val dx = x - ox * xscale + tpag.targetX * xscale
        val dy = y - oy * yscale + tpag.targetY * yscale
        val dw = tpag.sourceWidth * xscale
        val dh = tpag.sourceHeight * yscale

        // Color (BGR)
        val cr = (blend and 0xFF) / 255f
        val cg = ((blend shr 8) and 0xFF) / 255f
        val cb = ((blend shr 16) and 0xFF) / 255f

        glBindTexture(GL_TEXTURE_2D, texId)
        glColor4f(cr, cg, cb, alpha.toFloat())

        if (angle != 0.0) {
            glPushMatrix()
            glTranslated(x, y, 0.0)
            glRotated(-angle, 0.0, 0.0, 1.0)
            glTranslated(-x, -y, 0.0)
        }

        glBegin(GL_QUADS)
        glTexCoord2f(u0, v0); glVertex2d(dx, dy)
        glTexCoord2f(u1, v0); glVertex2d(dx + dw, dy)
        glTexCoord2f(u1, v1); glVertex2d(dx + dw, dy + dh)
        glTexCoord2f(u0, v1); glVertex2d(dx, dy + dh)
        glEnd()

        if (angle != 0.0) {
            glPopMatrix()
        }
    }

    fun drawBackground(tpagIndex: Int, x: Int, y: Int, tileX: Boolean, tileY: Boolean) {
        if (tpagIndex < 0 || tpagIndex >= gameData.texturePageItems.size) return
        val tpag = gameData.texturePageItems[tpagIndex]

        val texId = ensureTexture(tpag.texturePageId)
        if (texId < 0) return

        val texW = textureWidths[tpag.texturePageId].toFloat()
        val texH = textureHeights[tpag.texturePageId].toFloat()
        if (texW == 0f || texH == 0f) return

        val u0 = tpag.sourceX / texW
        val v0 = tpag.sourceY / texH
        val u1 = (tpag.sourceX + tpag.sourceWidth) / texW
        val v1 = (tpag.sourceY + tpag.sourceHeight) / texH

        glBindTexture(GL_TEXTURE_2D, texId)
        glColor4f(1f, 1f, 1f, 1f)

        if (tileX || tileY) {
            val w = tpag.sourceWidth
            val h = tpag.sourceHeight
            val startX = if (tileX) ((x % w) - w) else x
            val startY = if (tileY) ((y % h) - h) else y
            val endX = if (tileX) (viewX + viewW) else (x + w)
            val endY = if (tileY) (viewY + viewH) else (y + h)

            var cy = startY
            while (cy < endY) {
                var cx = startX
                while (cx < endX) {
                    glBegin(GL_QUADS)
                    glTexCoord2f(u0, v0); glVertex2i(cx, cy)
                    glTexCoord2f(u1, v0); glVertex2i(cx + w, cy)
                    glTexCoord2f(u1, v1); glVertex2i(cx + w, cy + h)
                    glTexCoord2f(u0, v1); glVertex2i(cx, cy + h)
                    glEnd()
                    cx += w
                }
                cy += h
            }
        } else {
            glBegin(GL_QUADS)
            glTexCoord2f(u0, v0); glVertex2i(x, y)
            glTexCoord2f(u1, v0); glVertex2i(x + tpag.sourceWidth, y)
            glTexCoord2f(u1, v1); glVertex2i(x + tpag.sourceWidth, y + tpag.sourceHeight)
            glTexCoord2f(u0, v1); glVertex2i(x, y + tpag.sourceHeight)
            glEnd()
        }
    }

    fun drawRectangle(x1: Double, y1: Double, x2: Double, y2: Double, outline: Boolean) {
        glBindTexture(GL_TEXTURE_2D, 0)
        val cr = (drawColor and 0xFF) / 255f
        val cg = ((drawColor shr 8) and 0xFF) / 255f
        val cb = ((drawColor shr 16) and 0xFF) / 255f
        glColor4f(cr, cg, cb, drawAlpha.toFloat())

        if (outline) {
            glBegin(GL_LINE_LOOP)
        } else {
            glBegin(GL_QUADS)
        }
        glVertex2d(x1, y1)
        glVertex2d(x2, y1)
        glVertex2d(x2, y2)
        glVertex2d(x1, y2)
        glEnd()
    }

    fun drawText(x: Double, y: Double, text: String) {
        if (drawFont < 0 || drawFont >= gameData.fonts.size) return
        val font = gameData.fonts[drawFont]
        if (font.tpagIndex < 0) return
        val tpag = gameData.texturePageItems[font.tpagIndex]

        val texId = ensureTexture(tpag.texturePageId)
        if (texId < 0) return

        val texW = textureWidths[tpag.texturePageId].toFloat()
        val texH = textureHeights[tpag.texturePageId].toFloat()
        if (texW == 0f || texH == 0f) return

        val cr = (drawColor and 0xFF) / 255f
        val cg = ((drawColor shr 8) and 0xFF) / 255f
        val cb = ((drawColor shr 16) and 0xFF) / 255f

        glBindTexture(GL_TEXTURE_2D, texId)
        glColor4f(cr, cg, cb, drawAlpha.toFloat())

        val lines = text.split("\n")
        val lineHeight = font.emSize.toDouble()
        var textHeight = lines.size * lineHeight

        var startY = when (drawValign) {
            1 -> y - textHeight / 2
            2 -> y - textHeight
            else -> y
        }

        for (line in lines) {
            val lineWidth = measureLineWidth(font, line)
            var cx = when (drawHalign) {
                1 -> x - lineWidth / 2
                2 -> x - lineWidth
                else -> x
            }

            for (ch in line) {
                val glyph = font.glyphs.find { it.character == ch.code } ?: continue
                val gx = tpag.sourceX + glyph.x
                val gy = tpag.sourceY + glyph.y
                val gu0 = gx / texW
                val gv0 = gy / texH
                val gu1 = (gx + glyph.width) / texW
                val gv1 = (gy + glyph.height) / texH

                val dx = cx + glyph.offset
                val dy = startY

                glBegin(GL_QUADS)
                glTexCoord2f(gu0, gv0); glVertex2d(dx, dy)
                glTexCoord2f(gu1, gv0); glVertex2d(dx + glyph.width, dy)
                glTexCoord2f(gu1, gv1); glVertex2d(dx + glyph.width, dy + glyph.height)
                glTexCoord2f(gu0, gv1); glVertex2d(dx, dy + glyph.height)
                glEnd()

                cx += glyph.shift
            }

            startY += lineHeight
        }
    }

    fun drawTextTransformed(x: Double, y: Double, text: String, xscale: Double, yscale: Double, angle: Double) {
        if (drawFont < 0 || drawFont >= gameData.fonts.size) return
        val font = gameData.fonts[drawFont]
        if (font.tpagIndex < 0) return
        val tpag = gameData.texturePageItems[font.tpagIndex]

        val texId = ensureTexture(tpag.texturePageId)
        if (texId < 0) return

        val texW = textureWidths[tpag.texturePageId].toFloat()
        val texH = textureHeights[tpag.texturePageId].toFloat()
        if (texW == 0f || texH == 0f) return

        val cr = (drawColor and 0xFF) / 255f
        val cg = ((drawColor shr 8) and 0xFF) / 255f
        val cb = ((drawColor shr 16) and 0xFF) / 255f

        glBindTexture(GL_TEXTURE_2D, texId)
        glColor4f(cr, cg, cb, drawAlpha.toFloat())

        if (angle != 0.0 || xscale != 1.0 || yscale != 1.0) {
            glPushMatrix()
            glTranslated(x, y, 0.0)
            if (angle != 0.0) glRotated(-angle, 0.0, 0.0, 1.0)
            if (xscale != 1.0 || yscale != 1.0) glScaled(xscale, yscale, 1.0)
            glTranslated(-x, -y, 0.0)
        }

        var cx = x
        for (ch in text) {
            val glyph = font.glyphs.find { it.character == ch.code } ?: continue
            val gx = tpag.sourceX + glyph.x
            val gy = tpag.sourceY + glyph.y
            val gu0 = gx / texW
            val gv0 = gy / texH
            val gu1 = (gx + glyph.width) / texW
            val gv1 = (gy + glyph.height) / texH

            val dx = cx + glyph.offset
            val dy = y

            glBegin(GL_QUADS)
            glTexCoord2f(gu0, gv0); glVertex2d(dx, dy)
            glTexCoord2f(gu1, gv0); glVertex2d(dx + glyph.width, dy)
            glTexCoord2f(gu1, gv1); glVertex2d(dx + glyph.width, dy + glyph.height)
            glTexCoord2f(gu0, gv1); glVertex2d(dx, dy + glyph.height)
            glEnd()

            cx += glyph.shift
        }

        if (angle != 0.0 || xscale != 1.0 || yscale != 1.0) {
            glPopMatrix()
        }
    }

    fun measureStringWidth(text: String): Double {
        if (drawFont < 0 || drawFont >= gameData.fonts.size) return 0.0
        val font = gameData.fonts[drawFont]
        return text.split("\n").maxOfOrNull { measureLineWidth(font, it) } ?: 0.0
    }

    fun measureStringHeight(text: String): Double {
        if (drawFont < 0 || drawFont >= gameData.fonts.size) return 0.0
        val font = gameData.fonts[drawFont]
        return text.split("\n").size * font.emSize.toDouble()
    }

    private fun measureLineWidth(font: com.mrpowergamerbr.kgmsruntime.data.FontData, line: String): Double {
        var w = 0.0
        for (ch in line) {
            val glyph = font.glyphs.find { it.character == ch.code }
            w += glyph?.shift?.toDouble() ?: 0.0
        }
        return w
    }

    private fun ensureTexture(pageId: Int): Int {
        if (pageId < 0 || pageId >= textures.size) return -1
        if (textures[pageId] >= 0) return textures[pageId]

        val page = gameData.texturePages[pageId]
        val pngData = ByteArray(page.pngLength)
        val fileBuf = gameData.fileBuffer
        for (i in 0 until page.pngLength) {
            pngData[i] = fileBuf.get(page.pngOffset + i)
        }

        val directBuf = ByteBuffer.allocateDirect(pngData.size)
        directBuf.put(pngData)
        directBuf.flip()

        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val comp = stack.mallocInt(1)

            val pixels = stbi_load_from_memory(directBuf, w, h, comp, 4)
            if (pixels == null) {
                System.err.println("Failed to decode texture page $pageId: ${stbi_failure_reason()}")
                return -1
            }

            val texId = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texId)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

            textureWidths[pageId] = w.get(0)
            textureHeights[pageId] = h.get(0)
            textures[pageId] = texId

            stbi_image_free(pixels)
            println("  Loaded texture page $pageId: ${w.get(0)}x${h.get(0)}")
            return texId
        }
    }
}
