package com.icil.dell.d3_view;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.MotionEvent;

import com.threed.jpct.*;
import com.threed.jpct.util.MemoryHelper;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {


    private static MainActivity master = null;

    private GLSurfaceView mGLView;
    private MyRenderer renderer = null;
    private FrameBuffer fb = null;
    private World world = null;
    private RGBColor back = new RGBColor(50,50,100);


    private float touchTurn = 0;
    private float touchTurnUp = 0;

    private float xpos = -1;
    private float ypos = -1;


   // private Object3D selectedObj;

    private Object3D n_dome_3;
    private Object3D n_field_3;
    private Object3D plane;


    private int fps = 0;

    private Light sun = null;



    protected void onCreate(Bundle savedInstanceState) {

        Logger.log("onCreate");

        if (master != null) {
            copy(master);
        }

        super.onCreate(savedInstanceState);
        mGLView = new GLSurfaceView(getApplication());

        mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {

            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

                int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
                EGLConfig[] configs = new EGLConfig[1];
                int[] result = new int[1];
                egl.eglChooseConfig(display, attributes, configs, 1, result);
                return configs[0];
            }
        });

        renderer = new MyRenderer();
        mGLView.setRenderer(renderer);
        setContentView(mGLView);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void copy(Object src) {
        try {
            Logger.log("Copying data from master Activity!");
            Field[] fs = src.getClass().getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
                f.set(this, f.get(src));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public boolean onTouchEvent(MotionEvent me) {

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            xpos = me.getX();
            ypos = me.getY();
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_UP) {
            xpos = -1;
            ypos = -1;
            touchTurn = 0;
            touchTurnUp = 0;
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            float xd = me.getX() - xpos;
            float yd = me.getY() - ypos;

            xpos = me.getX();
            ypos = me.getY();

            touchTurn = xd / -80f;   ///roation speed
            touchTurnUp = yd / -100f;
            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {

        }

        return super.onTouchEvent(me);
    }

    protected boolean isFullscreenOpaque() {
        return true;
    }



    class MyRenderer implements GLSurfaceView.Renderer {

        private long time = System.currentTimeMillis();

     public MyRenderer() {

     }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
            gl.glClearDepthf(1.0f);
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);

            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int w, int h) {

            if (fb != null) {
                fb.dispose();
            }

            fb = new FrameBuffer(gl, w, h);

            if (master == null) {

                world = new World();
                world.setAmbientLight(50, 50, 50);


                sun = new Light(world);
                sun.setIntensity(250, 250, 250);

                TextureManager tm=TextureManager.getInstance();

                Texture sky3 =new Texture(getResources().openRawResource(R.raw.sky3));
                tm.addTexture("sky3",sky3);

                Texture grass = new Texture(getResources().openRawResource(R.raw.grass));
                tm.addTexture("grass",grass);




                Object3D[] objectsArray2 = new Object3D[0];
                try {
                    objectsArray2 = Loader.loadOBJ(getResources().getAssets().open("n_dome_3.obj"), getResources().getAssets().open("n_dome_3.mtl"), 1.0f);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Object3D[] objectsArray3 = new Object3D[0];
                try {
                    objectsArray3 = Loader.loadOBJ(getResources().getAssets().open("n_field_3.obj"), getResources().getAssets().open("n_field_3.mtl"), 1.0f);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                n_dome_3 = Object3D.mergeAll(objectsArray2);
                n_field_3 = Object3D.mergeAll(objectsArray3);




                n_dome_3.setTexture("sky3");
              n_field_3.setTexture("grass");


                n_dome_3.strip();
                n_field_3.strip();

                n_dome_3.build();
                n_field_3.build();

                n_dome_3.setOrigin(new SimpleVector(0, -100, -20));
                n_field_3.setOrigin(new SimpleVector(0, 15, -20));


                n_dome_3.rotateZ(0.0f);
                n_dome_3.rotateX(9.41f);
                n_dome_3.rotateY(0.0f);
                world.addObject(n_dome_3);

                n_field_3.rotateZ(0.0f);
                n_field_3.rotateX(-0.03f);
                n_field_3.rotateY(0.0f);
                world.addObject(n_field_3);

                Camera cam = world.getCamera();
               cam.moveCamera(Camera.CAMERA_MOVEOUT, 110); //zoom in


                cam.lookAt(n_dome_3.getTransformedCenter());
               // cam.setFOV(1.5f);
                SimpleVector sv = new SimpleVector();
                sv.set(n_field_3.getTransformedCenter());
                sv.y -= 50; ///set the light source dimension
                sv.z -= 50;
                sv.x -= 50;
                //sun.setPosition(sv);
                MemoryHelper.compact();

                if (master == null) {
                    Logger.log("Saving master Activity!");
                    master = MainActivity.this;
                }

            }



         /*   gl.glViewport(0, 0, w, h);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            GLU.gluPerspective(gl, 0.0f, (float) w / (float) h,
                    0.1f, 100.0f);

            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();*/




        }








        @Override
        public void onDrawFrame(GL10 gl) {





            if (touchTurn != 0) {
                n_field_3.rotateY(touchTurn);
                n_dome_3.rotateY(touchTurn);
                //plane.rotateY(touchTurn);
                touchTurn = 0;
            }

            if (touchTurnUp != 0) {
               //n_field_3.rotateX(touchTurnUp);
                //n_dome_3.rotateZ(touchTurnUp);
               // plane.rotateZ(touchTurnUp);
                touchTurnUp = 0;
            }

            fb.clear(back);
            world.renderScene(fb);
            world.draw(fb);
            fb.display();

            if (System.currentTimeMillis() - time >= 1000) {
                Logger.log(fps + "fps");
                fps = 0;
                time = System.currentTimeMillis();
            }
            fps++;
        }

        }



}
