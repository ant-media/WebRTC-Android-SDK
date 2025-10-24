package io.antmedia.webrtcandroidframework.canvas;

import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
interface ICanvasListener {
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config);
    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) ;
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl);
}

public class CanvasListener implements  ICanvasListener{
    @Override
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config){

    }
    @Override
    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height){

    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl){

    }
}
