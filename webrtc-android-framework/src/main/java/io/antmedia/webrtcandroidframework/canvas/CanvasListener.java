package io.antmedia.webrtcandroidframework.canvas;

interface ICanvasListener {
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config);

    void onSurfaceIntialized(javax.microedition.khronos.opengles.GL10 gl);

    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) ;
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl);
}

public class CanvasListener implements  ICanvasListener{
    @Override
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config){

    }
    @Override
    public void onSurfaceIntialized(javax.microedition.khronos.opengles.GL10 gl){

    }
    @Override
    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height){

    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl){

    }
}
