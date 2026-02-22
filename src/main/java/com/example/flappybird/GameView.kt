package com.example.flappybird

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val updateMillis: Long = 16

    private enum class GameState { START, PLAYING, GAME_OVER }
    private var gameState = GameState.START

    private var birdY = 600f
    private var velocity = 0f
    private val gravity = 1.4f
    private val jumpVelocity = -24f

    private val birdRadius = 40f
    private val pipeWidth = 170f
    private val pipeGap = 480f
    private var pipeX = 1000f
    private var topPipeHeight = 300f

    private var score = 0
    private var highScore = 0

    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Neon colors
    private val bgTop = Color.rgb(10, 10, 25)
    private val bgBottom = Color.rgb(25, 0, 40)
    private val neonGreen = Color.rgb(0, 255, 140)
    private val neonPink = Color.rgb(255, 0, 180)
    private val neonBlue = Color.rgb(0, 200, 255)

    private var bgGradient: LinearGradient? = null

    // Stars
    private data class Star(var x: Float, var y: Float, var speed: Float)
    private val stars = mutableListOf<Star>()

    // Particles
    private data class Particle(var x: Float, var y: Float, var dx: Float, var dy: Float)
    private val particles = mutableListOf<Particle>()

    // Screen shake
    private var shakeFrames = 0

    // Slow motion powerup
    private var slowMotion = false
    private var slowMotionTimer = 0

    private val runnable = object : Runnable {
        override fun run() {
            if (gameState == GameState.PLAYING) update()
            invalidate()
            handler.postDelayed(this, updateMillis)
        }
    }

    init {
        highScore = prefs.getInt("HIGH_SCORE", 0)
        handler.post(runnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bgGradient = LinearGradient(0f, 0f, 0f, h.toFloat(), bgTop, bgBottom, Shader.TileMode.CLAMP)

        stars.clear()
        repeat(100) {
            stars.add(
                Star(
                    Random.nextFloat() * w,
                    Random.nextFloat() * h,
                    Random.nextFloat() * 3 + 1
                )
            )
        }
    }

    private fun update() {

        val speedMultiplier = if (slowMotion) 0.5f else 1f

        velocity += gravity
        birdY += velocity

        pipeX -= 14 * speedMultiplier

        if (pipeX + pipeWidth < 0) {
            pipeX = width.toFloat()
            topPipeHeight = Random.nextInt(200, height - 700).toFloat()
            score++

            // 20% chance slow motion powerup
            if (Random.nextInt(5) == 0) {
                slowMotion = true
                slowMotionTimer = 180
            }
        }

        if (slowMotion) {
            slowMotionTimer--
            if (slowMotionTimer <= 0) slowMotion = false
        }

        // Ground
        if (birdY + birdRadius > height - 120) gameOver()

        // Ceiling
        if (birdY - birdRadius < 0) gameOver()

        // Pipe collision
        if (pipeX < 200 + birdRadius && pipeX + pipeWidth > 200 - birdRadius) {
            if (birdY - birdRadius < topPipeHeight ||
                birdY + birdRadius > topPipeHeight + pipeGap
            ) {
                gameOver()
            }
        }

        // Stars movement
        stars.forEach {
            it.y += it.speed
            if (it.y > height) it.y = 0f
        }

        // Particles
        particles.forEach {
            it.x += it.dx
            it.y += it.dy
        }

        if (shakeFrames > 0) shakeFrames--
    }

    private fun gameOver() {
        gameState = GameState.GAME_OVER
        shakeFrames = 15

        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("HIGH_SCORE", highScore).apply()
        }

        // Explosion
        repeat(40) {
            val angle = Random.nextFloat() * 360
            val speed = Random.nextFloat() * 15
            particles.add(
                Particle(
                    200f,
                    birdY,
                    cos(angle) * speed,
                    sin(angle) * speed
                )
            )
        }
    }

    private fun resetGame() {
        birdY = 600f
        velocity = 0f
        pipeX = width.toFloat()
        topPipeHeight = Random.nextInt(200, height - 700).toFloat()
        score = 0
        particles.clear()
        slowMotion = false
        gameState = GameState.PLAYING
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (shakeFrames > 0) {
            canvas.translate(
                Random.nextInt(-20, 20).toFloat(),
                Random.nextInt(-20, 20).toFloat()
            )
        }

        // Background
        paint.shader = bgGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Stars
        paint.color = Color.WHITE
        stars.forEach {
            canvas.drawCircle(it.x, it.y, 2f, paint)
        }

        when (gameState) {

            GameState.START -> drawStart(canvas)

            GameState.PLAYING -> drawGame(canvas)

            GameState.GAME_OVER -> {
                drawGame(canvas)
                drawGameOver(canvas)
            }
        }
    }

    private fun drawGame(canvas: Canvas) {

        // Pipes
        paint.color = neonGreen
        paint.setShadowLayer(25f, 0f, 0f, neonGreen)
        canvas.drawRect(pipeX, 0f, pipeX + pipeWidth, topPipeHeight, paint)
        canvas.drawRect(pipeX, topPipeHeight + pipeGap, pipeX + pipeWidth, height.toFloat(), paint)
        paint.clearShadowLayer()

        // Bird
        paint.color = neonPink
        paint.setShadowLayer(30f, 0f, 0f, neonPink)
        canvas.drawCircle(200f, birdY, birdRadius, paint)
        paint.clearShadowLayer()

        // Particles
        paint.color = neonBlue
        particles.forEach {
            canvas.drawCircle(it.x, it.y, 6f, paint)
        }

        // Score
        paint.color = Color.WHITE
        paint.textSize = 110f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(score.toString(), width / 2f - 30f, 150f, paint)
    }

    private fun drawStart(canvas: Canvas) {

        paint.color = neonGreen
        paint.textSize = 120f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("NEON BIRD", width / 5f, height / 3f, paint)

        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("Tap to Start", width / 3f, height / 2f, paint)

        paint.textSize = 50f
        canvas.drawText("High Score: $highScore", width / 3.5f, height / 2f + 80f, paint)
    }

    private fun drawGameOver(canvas: Canvas) {

        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRect(width / 6f, height / 3f, width - width / 6f, height / 2f, paint)

        paint.color = neonPink
        paint.textSize = 90f
        canvas.drawText("GAME OVER", width / 4f, height / 2.5f, paint)

        paint.color = Color.WHITE
        paint.textSize = 50f
        canvas.drawText("Tap to Restart", width / 3f, height / 2.2f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            when (gameState) {
                GameState.START -> gameState = GameState.PLAYING
                GameState.PLAYING -> velocity = jumpVelocity
                GameState.GAME_OVER -> resetGame()
            }

            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
