package com.engagecraft.gaming.epllastmanstanding.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import kotlin.math.abs
import kotlin.math.max

/**
 * The game WebView. Keeps host gestures from stealing its drags and, when [diagnostics] is on,
 * logs the touch stream it actually receives through [TouchDiagnostics].
 */
@SuppressLint("ViewConstructor")
internal class GameWebViewView(
    context: Context,
    private val diagnostics: Boolean,
) : WebView(context) {

    private var downX = 0f
    private var downY = 0f
    private var downScrollY = 0
    private var maxDx = 0f
    private var maxDy = 0f
    private var moves = 0
    private var clampedThisGesture = false
    private var lastScrollLog = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // A host ancestor (e.g. a drawer swipe) otherwise claims any drag that drifts a few dp
            // sideways and the WebView gets ACTION_CANCEL mid-scroll. In Compose this also moves
            // AndroidView dispatch ahead of ancestor gesture detectors. Reset on UP/CANCEL.
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (diagnostics) trace(event)
        return super.dispatchTouchEvent(event)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (diagnostics) TouchDiagnostics.log("child requestDisallowInterceptTouchEvent($disallowIntercept)")
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (diagnostics) {
            val chain = generateSequence(parent) { it.parent }.joinToString(" < ") { it.javaClass.name }
            TouchDiagnostics.log("attached; parents: $chain")
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (!diagnostics) return
        val now = SystemClock.uptimeMillis()
        if (now - lastScrollLog < SCROLL_LOG_THROTTLE_MS) return
        lastScrollLog = now
        TouchDiagnostics.log("native scroll y=$t (from $oldt)")
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        if (diagnostics && clampedY && !clampedThisGesture) {
            clampedThisGesture = true
            TouchDiagnostics.log("native overscroll clamped at y=$scrollY")
        }
    }

    private fun trace(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downScrollY = scrollY
                maxDx = 0f
                maxDy = 0f
                moves = 0
                clampedThisGesture = false
                TouchDiagnostics.log(
                    "DOWN x=${event.x.toInt()} y=${event.y.toInt()} scrollY=$scrollY " +
                        "parent=${parent?.javaClass?.name}",
                )
            }

            MotionEvent.ACTION_MOVE -> {
                moves++
                maxDx = max(maxDx, abs(event.x - downX))
                maxDy = max(maxDy, abs(event.y - downY))
            }

            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP ->
                TouchDiagnostics.log("POINTER pointers=${event.pointerCount}")

            MotionEvent.ACTION_UP -> TouchDiagnostics.log("UP ${summary(event)}")

            MotionEvent.ACTION_CANCEL -> TouchDiagnostics.warn(
                "CANCEL ${summary(event)}",
                Throwable("ACTION_CANCEL delivered to the game WebView"),
            )
        }
    }

    private fun summary(event: MotionEvent): String =
        "moves=$moves dx=${(event.x - downX).toInt()} dy=${(event.y - downY).toInt()} " +
            "maxDx=${maxDx.toInt()} maxDy=${maxDy.toInt()} scrollY=$downScrollY->$scrollY"

    private companion object {
        const val SCROLL_LOG_THROTTLE_MS = 250L
    }
}
