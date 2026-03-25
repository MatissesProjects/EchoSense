package com.echosense.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FrequencyVisualizerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val spectrumPaint = Paint().apply {
        color = Color.parseColor("#4400E5FF") // Transparent Neon Cyan
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val curvePaint = Paint().apply {
        color = Color.parseColor("#1DE9B6") // Neon Teal
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1AFFFFFF") // Very subtle white grid
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val isolationPaint = Paint().apply {
        color = Color.parseColor("#FF9800") // Vibrant Orange
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private var fftData: FloatArray = FloatArray(0)
    private var eqCurveData: FloatArray = FloatArray(0)
    private var isolationGainDb: Float = 0.0f

    fun updateData(fft: FloatArray, curve: FloatArray, isolation: Float = 0.0f) {
        fftData = fft
        eqCurveData = curve
        isolationGainDb = isolation
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        drawGrid(canvas)
        drawSpectrum(canvas)
        drawEqCurve(canvas)
        drawIsolationText(canvas)
    }

    private fun drawIsolationText(canvas: Canvas) {
        if (isolationGainDb > 0.1f) {
            val text = String.format("AI ISOLATION: +%.1f dB", isolationGainDb)
            canvas.drawText(text, width - 350f, 50f, isolationPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // Vertical grid lines for 100, 1k, 10k Hz
        val frequencies = listOf(100f, 1000f, 10000f)
        frequencies.forEach { f ->
            val x = frequencyToX(f)
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
    }

    private fun drawSpectrum(canvas: Canvas) {
        if (fftData.isEmpty()) return
        
        val path = Path()
        path.moveTo(0f, height.toFloat())
        
        val step = width.toFloat() / fftData.size
        for (i in fftData.indices) {
            val x = i * step
            // Logarithmic vertical scale for spectrum
            val y = height - (Math.log10(fftData[i].toDouble() + 1.0) * height * 2.0).toFloat()
            path.lineTo(x, y.coerceIn(0f, height.toFloat()))
        }
        
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()
        canvas.drawPath(path, spectrumPaint)
    }

    private fun drawEqCurve(canvas: Canvas) {
        if (eqCurveData.isEmpty()) return
        
        val path = Path()
        val step = width.toFloat() / eqCurveData.size
        
        for (i in eqCurveData.indices) {
            val x = i * step
            // 1.0 magnitude is middle of the view
            val y = height / 2f - (Math.log10(eqCurveData[i].toDouble()).toFloat() * height / 2f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        canvas.drawPath(path, curvePaint)
    }

    private fun frequencyToX(f: Float): Float {
        // Simple log scale mapping
        val minF = 20f
        val maxF = 20000f
        val logF = Math.log10(f.toDouble()).toFloat()
        val logMin = Math.log10(minF.toDouble()).toFloat()
        val logMax = Math.log10(maxF.toDouble()).toFloat()
        return (logF - logMin) / (logMax - logMin) * width
    }
}