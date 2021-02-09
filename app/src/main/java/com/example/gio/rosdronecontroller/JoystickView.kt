package com.example.gio.rosdronecontroller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

/**
 * Created by gio on 12/12/17.
 */
class JoystickView : SurfaceView, SurfaceHolder.Callback, View.OnTouchListener {


    private var centerX : Float = 0.0f
    private var centerY : Float = 0.0f
    private var baseRadius : Float = 0.0f
    private var capRadius : Float = 0.0f

    private lateinit var joystickListenerCallback: JoystickListener

    private lateinit var upIcon: Bitmap
    private lateinit var upRect: Pair<Float, Float>
    private lateinit var rightIcon: Bitmap
    private lateinit var rightRect: Pair<Float, Float>
    private lateinit var downIcon: Bitmap
    private lateinit var downRect: Pair<Float, Float>
    private lateinit var leftIcon: Bitmap
    private lateinit var leftRect: Pair<Float, Float>

    constructor(context: Context): super(context) {
        this.holder.addCallback(this)
        setOnTouchListener(this)
        if (context is JoystickListener) {
            this.joystickListenerCallback = context
        }
    }

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet) {
        this.holder.addCallback(this)
        setOnTouchListener(this)
        if (context is JoystickListener) {
            this.joystickListenerCallback = context
        }
    }

    constructor(context: Context, attributeSet: AttributeSet, style:Int):
            super(context, attributeSet, style) {
        this.holder.addCallback(this)
        setOnTouchListener(this)
        if (context is JoystickListener) {
            this.joystickListenerCallback = context
        }
    }

    private fun setupDimensions() {
        centerX =  (width / 2).toFloat()
        centerY = (height / 2).toFloat()
        baseRadius = (Math.min(width, height) * 0.40).toFloat()
        capRadius = (Math.min(width, height) / 12).toFloat()

    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (holder != null) {
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }

        setupDimensions()

        // Icon initialization
        if (id == R.id.leftJoystickView) {
            upIcon = BitmapFactory.decodeResource(resources, R.drawable.if_nav_arrow_up)
            upIcon = Bitmap.createScaledBitmap(
                    upIcon, upIcon.width/20, upIcon.height/20, false)
            upRect = Pair(centerX - upIcon.width/2, (centerY - (baseRadius * 0.9)).toFloat())

            rightIcon = BitmapFactory.decodeResource(resources, R.drawable.if_rotate_right)
            rightIcon = Bitmap.createScaledBitmap(
                    rightIcon, rightIcon.width/15, rightIcon.height/15, false)
            rightRect = Pair((centerX + (baseRadius * 0.8) - rightIcon.width/2).toFloat(), centerY - rightIcon.height/2)

            downIcon = BitmapFactory.decodeResource(resources, R.drawable.if_nav_arrow_down)
            downIcon = Bitmap.createScaledBitmap(
                    downIcon, downIcon.width/20, downIcon.height/20, false)
            downRect = Pair(centerX - downIcon.width/2, (centerY + (baseRadius * 0.8) - downIcon.height/2).toFloat())

            leftIcon = BitmapFactory.decodeResource(resources, R.drawable.if_rotate_left)
            leftIcon = Bitmap.createScaledBitmap(leftIcon, leftIcon.width/15, leftIcon.height/15, false)
            leftRect = Pair((centerX - (baseRadius * 1)), centerY - leftIcon.height/2)

        } else if (id == R.id.rightJoystickView) {
            upIcon = BitmapFactory.decodeResource(resources, R.drawable.if_double_arrow_up)
            upIcon = Bitmap.createScaledBitmap(
                    upIcon, upIcon.width/20, upIcon.height/20, false)
            upRect = Pair(centerX - upIcon.width/2, (centerY - (baseRadius * 0.95)).toFloat())

            rightIcon = BitmapFactory.decodeResource(resources, R.drawable.if_nav_arrow_right)
            rightIcon = Bitmap.createScaledBitmap(
                    rightIcon, rightIcon.width/15, rightIcon.height/15, false)
            rightRect = Pair((centerX + (baseRadius * 0.8) - rightIcon.width/2).toFloat(), centerY - rightIcon.height/2)

            downIcon = BitmapFactory.decodeResource(resources, R.drawable.if_double_arrow_down)
            downIcon = Bitmap.createScaledBitmap(
                    downIcon, downIcon.width/20, downIcon.height/20, false)
            downRect = Pair(centerX - downIcon.width/2, (centerY + (baseRadius * 0.75) - downIcon.height/2).toFloat())

            leftIcon = BitmapFactory.decodeResource(resources, R.drawable.if_nav_arrow_left)
            leftIcon = Bitmap.createScaledBitmap(leftIcon, leftIcon.width/15, leftIcon.height/15, false)
            leftRect = Pair((centerX - (baseRadius * 0.90)).toFloat(), centerY - leftIcon.height/2)
        }


        drawJoystick(centerX, centerY)

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (v != null && v.equals(this)) {
            if (event != null && event.action != MotionEvent.ACTION_UP) {
                val joystickDisplacement = Math.sqrt(Math.pow((event.x - centerX).toDouble(), 2.0) +
                        Math.pow((event.y - centerY).toDouble(), 2.0))
                if (joystickDisplacement < baseRadius) {
                    drawJoystick(event.x, event.y)
                } else {
                    val ratio : Float = (baseRadius / joystickDisplacement).toFloat()
                    val constrainedX : Float = centerX + (event.x - centerX) * ratio
                    val constrainedY : Float = centerY + (event.y - centerY) * ratio
                    drawJoystick(constrainedX, constrainedY)
                }
            } else {
                drawJoystick(centerX, centerY)
            }
        }
        return true
    }

    private fun drawJoystick(newX: Float, newY: Float) {
        if (holder.surface.isValid) {
            var surfaceViewCanvas = this.holder.lockCanvas()

            var colors = Paint()

            surfaceViewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            var a = TypedValue()
            this.context.theme.resolveAttribute(android.R.attr.windowBackground , a, true)
            colors.setARGB(255, 250, 250, 250)
            surfaceViewCanvas.drawColor(colors.color)

            colors.setARGB(255, 192, 192, 192)
            surfaceViewCanvas.drawCircle(centerX, centerY, baseRadius, colors)

            /* Draw the icons on top of the circle */
            surfaceViewCanvas.drawBitmap(upIcon, upRect.first, upRect.second, Paint())

            surfaceViewCanvas.drawBitmap(rightIcon, rightRect.first, rightRect.second, Paint())

            surfaceViewCanvas.drawBitmap(downIcon, downRect.first, downRect.second, Paint())

            surfaceViewCanvas.drawBitmap(leftIcon, leftRect.first, leftRect.second, Paint())

            /*Calculate sector*/
            var h = Math.sqrt(Math.pow((newX - centerX).toDouble(), 2.0) + Math.pow((newY - centerY).toDouble(), 2.0))
            var sin = (newY - centerY) / h
            var cos = (newX - centerX) / h

            var deg = Math.toDegrees(Math.atan2(sin, cos))
            deg = if (deg < 0) (360 + deg) else deg
            colors.setARGB(100, 255, 255, 255)

            if (deg >= 315 || (deg >= 0 && deg <= 45)) {
                surfaceViewCanvas.drawArc(centerX - baseRadius, centerY - baseRadius, centerX + baseRadius, centerY + baseRadius,
                        315f, 90f, true, colors)
                this.joystickListenerCallback.onJoystickMoved(2, id)
            } else if ((deg >= 45 && deg <= 135)) {
                surfaceViewCanvas.drawArc(centerX - baseRadius, centerY - baseRadius, centerX + baseRadius, centerY + baseRadius,
                        45f, 90f, true, colors)
                this.joystickListenerCallback.onJoystickMoved(3, id)
            } else if (deg >= 135 && deg <= 225) {
                surfaceViewCanvas.drawArc(centerX - baseRadius, centerY - baseRadius, centerX + baseRadius, centerY + baseRadius,
                        135f, 90f, true, colors)
                this.joystickListenerCallback.onJoystickMoved(4, id)
            } else if (deg >= 225 && deg <= 315) {
                surfaceViewCanvas.drawArc(centerX - baseRadius, centerY - baseRadius, centerX + baseRadius, centerY + baseRadius,
                        225f, 90f, true, colors)
                this.joystickListenerCallback.onJoystickMoved(1, id)
            } else {
                this.joystickListenerCallback.onJoystickMoved(0, id)
            }


            /* Draw joystick */
            colors.setARGB(255, 0, 0, 0)
            surfaceViewCanvas.drawCircle(newX, newY, capRadius, colors)


            this.holder.unlockCanvasAndPost(surfaceViewCanvas)
        }
    }

    interface JoystickListener {
        fun onJoystickMoved(region: Int, id: Int)
    }
}