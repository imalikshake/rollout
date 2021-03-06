package com.ammolite.rollout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class ThumbstickControl extends View
{
    private class Nub
    {
        float angle;
        float magnitude;

        public Nub()
        {
            //Initialise the member variables
            angle = 0;
            magnitude = 0;
        }

        void setPosition(float x, float y, float controlRadius)
        {
            //Get relative positions
            float xRel = x - centre.x;
            float yRel = y - centre.y;

            //Set angle and magnitude
            angle = (float)Math.atan(yRel / xRel);
            magnitude = (float)Math.sqrt(Math.pow(xRel,2) + Math.pow(yRel,2));

            //Limit the magnitude
            if (magnitude > controlRadius*0.5)
            {
                magnitude = (float)(controlRadius*0.5 + (magnitude-controlRadius*0.5)*0.3);
            }

            //Correct for negative values
            angle += xRel < 0? Math.PI : 0;
        }

        Point getPosition()
        {
            //Get the x and y components
            Double xComponent = magnitude * Math.cos(angle);
            Double yComponent = magnitude * Math.sin(angle);

            //Return the result as a point
            return new Point(xComponent.intValue(), yComponent.intValue());
        }
    }

    //private static float DEADZONE_MAGNITUDE = 50.0f;
    public static final float DEAD_ZONE_MAGNITUDE = 50.0f;

    Paint backgroundPaint, nubPaint;
    Nub nub;
    Point centre;
    boolean nubHeld;

    private Runnable animator = new Runnable()
    {
        @Override
        public void run()
        {
            if (Math.abs(nub.magnitude) > 0.5 && !nubHeld)
            {
                postDelayed(this, 15);
                invalidate();
            }

            /*if (nub.magnitude < DEADZONE_MAGNITUDE && senderThread.isAlive()) {
                keepRunning = false;
                Log.d("TESTINGTHREAD", "Stopped");
            }*/
        }
    };

    public float getAbsoluteMagnitude() {
        return Math.abs(nub.magnitude);
    }

    public float getAngle() {
        return nub.angle;
    }

    public ThumbstickControl(Context context)
    {
        super(context);
        setId(R.id.thumbstick);

        //Define the paints
        backgroundPaint = new Paint();
        backgroundPaint.setColor(getResources().getColor(R.color.red));
        nubPaint = new Paint();
        nubPaint.setColor(getResources().getColor(R.color.white));

        //Initialise the nub
        nub = new Nub();

//        senderThread = new Thread();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        //Redefine the centre
        centre = new Point(w/2,h/2);

        //Set the size of the control
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = this.getHeight();
        params.height = this.getHeight();
        this.setLayoutParams(params);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        //Draw the background
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, (float)(canvas.getWidth()*0.45), backgroundPaint);

        //Update the nub
        nub.magnitude -= nub.magnitude/12;

        //Draw the nub
        canvas.drawCircle(centre.x + nub.getPosition().x, centre.y + nub.getPosition().y,  (float)(canvas.getWidth()* (nubHeld?0.12:0.13)), nubPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //Get the position of the touch event
        nub.setPosition(event.getX(), event.getY(), this.getHeight()/2);

        //Determine if this is the last call for onTouch and therefore if the nub is being held
        nubHeld = event.getActionMasked() != MotionEvent.ACTION_UP;

        // Manage server sending thread.
        /*if (nub.magnitude > DEADZONE_MAGNITUDE) {
            if (!senderThread.isAlive()) {
                keepRunning = true;
                // Setup server communication thread
                senderThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("TESTINGTHREAD", "Started thread.");
                        while(keepRunning) {
                            // Blocked out at the moment as not guaranteed to have a connection.
                            //Sphero.shoot(nub.angle);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Log.d("THUMBSTICK_CONTROL", "Thread interrupted when attempting to sleep.", ex);
                            }
                        }
                        Log.d("TESTINGTHREAD", "While ended");
                    }
                });
                senderThread.start();
            }
        } else if (senderThread.isAlive()) {
            keepRunning = false;
            Log.d("TESTINGTHREAD", "Stopped");
        }*/

        //Depending on if the nub is being held, either just redraw or animate returning to centre
        if (nubHeld) invalidate();
        else animator.run();

        return true;
    }
}
