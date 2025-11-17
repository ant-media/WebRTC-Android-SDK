package io.antmedia.webrtcandroidframework.canvas;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageProxy;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;
import org.webrtc.YuvConverter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import io.antmedia.webrtcandroidframework.core.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.core.WebRTCClient;

public class ImageProxyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "ImageProxyRenderer";

    // Fullscreen quad (triangle strip)
    private static final float[] POSITIONS = new float[]{
            -1f,  1f, 0f,
            1f,  1f, 0f,
            -1f, -1f, 0f,
            1f, -1f, 0f
    };
    private static final float[] TEXCOORDS = new float[]{
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
    };
    private static final float[] MIRROREDTEXTCORD = new float[]{
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
    };
    // Flipped texture coordinates for drawing FBO texture (flip Y to fix upside-down)
    private static final float[] FLIPPED_TEXCOORDS = new float[]{
            0f, 1f,  // bottom-left
            1f, 1f,  // bottom-right
            0f, 0f,  // top-left
            1f, 0f   // top-right
    };

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "uniform mat3 uTexTransform;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main(){\n" +
                    "  gl_Position = aPosition;\n" +
                    "  vec3 t = uTexTransform * vec3(aTexCoord, 1.0);\n" +
                    "  vTexCoord = t.xy;\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform sampler2D y_tex;\n" +
                    "uniform sampler2D u_tex;\n" +
                    "uniform sampler2D v_tex;\n" +
                    "void main(){\n" +
                    "  float y = texture2D(y_tex, vTexCoord).r;\n" +
                    "  float u = texture2D(u_tex, vTexCoord).r - 0.5;\n" +
                    "  float v = texture2D(v_tex, vTexCoord).r - 0.5;\n" +
                    "  float r = y + 1.402 * v;\n" +
                    "  float g = y - 0.344136 * u - 0.714136 * v;\n" +
                    "  float b = y + 1.772 * u;\n" +
                    "  gl_FragColor = vec4(r, g, b, 1.0);\n" +
                    "}";

    // Simple shader for drawing a texture to screen (for display)
    private static final String TEXTURE_VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main(){\n" +
                    "  gl_Position = aPosition;\n" +
                    "  vTexCoord = aTexCoord;\n" +
                    "}";

    private static final String TEXTURE_FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "void main(){\n" +
                    "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
                    "}";

    private FloatBuffer positionBuffer;
    private FloatBuffer texCordBuffer;
    private FloatBuffer normalTexCordBuffer; // Alias for texCordBuffer
    private FloatBuffer mirroredTexCoordBuffer;
    private FloatBuffer flippedTexCoordBuffer;

    private int program = -1;
    private int aPositionLoc = -1;
    private int aTexCoordLoc = -1;
    private int uYLoc = -1, uULoc = -1, uVLoc = -1;
    private int uTexTransformLoc = -1;

    // Texture display program (for drawing FBO texture to screen)
    private int textureProgram = -1;
    private int textureAPositionLoc = -1;
    private int textureATexCoordLoc = -1;
    private int textureUTextureLoc = -1;

    private int texY = 0, texU = 0, texV = 0;
    private boolean texturesInitialized = false;

    // Latest frame data copied from ImageProxy
    private final ReentrantLock frameLock = new ReentrantLock();
    private ByteBuffer yData; // size width*height
    private ByteBuffer uData; // size (width/2)*(height/2)
    private ByteBuffer vData; // size (width/2)*(height/2)
    public int frameWidth = 0, frameHeight = 0;
    private boolean frameAvailable = false;

    // Transform controls
    private int rotationDegrees = 0; // 0, 90, 180, 270
    private boolean mirrorX = false; // mirror horizontally
    private final float[] texTransform = identity3();
    public int surfaceWidth = 0, surfaceHeight = 0;
    private WebRTCClient webRTCClient;
    private boolean callInProgress = false;
    private YuvConverter yuvConverter = new YuvConverter();
    private static Handler eglHandler;

    // Persistent FBO and texture for rendered frame (video + overlays)
    private int renderedFbo = 0;
    private int renderedTexture = 0;
    private int renderedTextureWidth = 0;
    private int renderedTextureHeight = 0;

    public ImageProxyRenderer() {}

    public Activity context;
    public CanvasListener listener;
    public SurfaceView surfaceView;
    public int currentOrientation = 0;
    public ImageProxyRenderer(WebRTCClient webRTCClient, Activity context,SurfaceView surfaceView,CanvasListener listener) {
        this.webRTCClient = webRTCClient;
        this.context = context;
        this.listener = listener;
        this.surfaceView = surfaceView;

        OrientationEventListener orientationEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {

                if ((orientation >= 330 || orientation < 30) && currentOrientation != 0) {
                    currentOrientation = 0;
                    listener.onOrientationChanged(0);

                } else if ((orientation >= 60 && orientation < 120) && currentOrientation != 90) {
                    currentOrientation = 90;
                    listener.onOrientationChanged(90);

                } else if ((orientation >= 150 && orientation < 210) && currentOrientation != 180) {
                    currentOrientation = 180;
                    listener.onOrientationChanged(180);

                } else if ((orientation >= 240 && orientation < 300) && currentOrientation != 270) {
                    currentOrientation = 270;
                    listener.onOrientationChanged(270);

                }

                if((currentOrientation == 90 || currentOrientation == 270) && CameraProviderHelper.lensFacing == CameraSelector.LENS_FACING_FRONT){
                    texCordBuffer = mirroredTexCoordBuffer;
                }
                else {
                    texCordBuffer = normalTexCordBuffer;
                }
                for (Overlay overlay : Overlay.overlayArray) {
                    overlay.setRotation(currentOrientation);
                }
            }

        };
        orientationEventListener.enable();
    }

    public void setCallInProgress(boolean callInProgress) {
        this.callInProgress = callInProgress;
    }

    public static void setEglHandler(Handler handler) {
        eglHandler = handler;
    }


    private void setMirror(boolean mirror) {
        mirrorX = mirror;
        updateTexTransform();
    }

    public void setRotationDegrees(int degrees) {
        rotationDegrees = ((degrees % 360) + 360) % 360;
        updateTexTransform();
    }

    public void submitImage(@NonNull ImageProxy image) {
        final Rect crop = image.getCropRect();
        // Use crop rect to avoid CameraX-imposed center crop causing apparent zoom
        final int width = crop.width();
        final int height = crop.height();
        setMirror(true);
        // For front camera (mirrored), use clockwise rotation; for back, counter-rotate.
        setRotationDegrees(false ? 180 : -180);

        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        int yRowStride = yPlane.getRowStride();
        int uRowStride = uPlane.getRowStride();
        int vRowStride = vPlane.getRowStride();
        int yPixelStride = yPlane.getPixelStride();
        int uPixelStride = uPlane.getPixelStride();
        int vPixelStride = vPlane.getPixelStride();

        int chromaWidth = width / 2;
        int chromaHeight = height / 2;
        // Offsets into planes for crop rect (guaranteed even for 420)
        int cropLeft = crop.left;
        int cropTop = crop.top;
        int chromaCropLeft = cropLeft / 2;
        int chromaCropTop = cropTop / 2;

        // Ensure buffers allocated
        frameLock.lock();
        try {
            if (frameWidth != width || frameHeight != height || yData == null) {
                yData = ByteBuffer.allocateDirect(width * height).order(ByteOrder.nativeOrder());
                uData = ByteBuffer.allocateDirect(chromaWidth * chromaHeight).order(ByteOrder.nativeOrder());
                vData = ByteBuffer.allocateDirect(chromaWidth * chromaHeight).order(ByteOrder.nativeOrder());
                frameWidth = width;
                frameHeight = height;
                texturesInitialized = false; // re-init texture sizes on next draw
            }

            yData.position(0);
            uData.position(0);
            vData.position(0);

            ByteBuffer yBuf = yPlane.getBuffer();
            for (int row = 0; row < height; row++) {
                int srcRow = cropTop + row;
                int rowStart = srcRow * yRowStride + cropLeft * yPixelStride;
                if (yPixelStride == 1) {
                    // Fast path: contiguous bytes
                    int oldPos = yBuf.position();
                    yBuf.position(rowStart);
                    for (int col = 0; col < width; col++) {
                        yData.put(yBuf.get());
                    }
                    yBuf.position(oldPos);
                } else {
                    for (int col = 0; col < width; col++) {
                        yData.put(yBuf.get(rowStart + col * yPixelStride));
                    }
                }
            }

            ByteBuffer uBuf = uPlane.getBuffer();
            ByteBuffer vBuf = vPlane.getBuffer();
            for (int row = 0; row < chromaHeight; row++) {
                int srcRow = chromaCropTop + row;
                for (int col = 0; col < chromaWidth; col++) {
                    int srcCol = chromaCropLeft + col;
                    uData.put(uBuf.get(srcRow * uRowStride + srcCol * uPixelStride));
                    vData.put(vBuf.get(srcRow * vRowStride + srcCol * vPixelStride));
                }
            }

            yData.position(0);
            uData.position(0);
            vData.position(0);
            frameAvailable = true;
        } finally {
            frameLock.unlock();
        }
    }

    @Override
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);

        positionBuffer = ByteBuffer.allocateDirect(POSITIONS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        positionBuffer.put(POSITIONS).position(0);

        texCordBuffer = ByteBuffer.allocateDirect(TEXCOORDS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCordBuffer.put(TEXCOORDS).position(0);
        normalTexCordBuffer = texCordBuffer;

        mirroredTexCoordBuffer = ByteBuffer.allocateDirect(MIRROREDTEXTCORD.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mirroredTexCoordBuffer.put(MIRROREDTEXTCORD).position(0);

        flippedTexCoordBuffer = ByteBuffer.allocateDirect(FLIPPED_TEXCOORDS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        flippedTexCoordBuffer.put(FLIPPED_TEXCOORDS).position(0);


        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (program == 0) throw new RuntimeException("Program creation failed");
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition");
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord");
        uYLoc = GLES20.glGetUniformLocation(program, "y_tex");
        uULoc = GLES20.glGetUniformLocation(program, "u_tex");
        uVLoc = GLES20.glGetUniformLocation(program, "v_tex");
        uTexTransformLoc = GLES20.glGetUniformLocation(program, "uTexTransform");

        // Create texture display program
        textureProgram = createProgram(TEXTURE_VERTEX_SHADER, TEXTURE_FRAGMENT_SHADER);
        if (textureProgram == 0) throw new RuntimeException("Texture program creation failed");
        textureAPositionLoc = GLES20.glGetAttribLocation(textureProgram, "aPosition");
        textureATexCoordLoc = GLES20.glGetAttribLocation(textureProgram, "aTexCoord");
        textureUTextureLoc = GLES20.glGetUniformLocation(textureProgram, "uTexture");

        int[] textures = new int[3];
        GLES20.glGenTextures(3, textures, 0);
        texY = textures[0];
        texU = textures[1];
        texV = textures[2];

        setupLuminanceTexture(texY);
        setupLuminanceTexture(texU);
        setupLuminanceTexture(texV);

        texturesInitialized = false; // will initialize dimensions on first frame

        setMirror(false);
    }

    @Override
    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
        listener.onSurfaceChanged(gl,width,height);
    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        boolean upload = false;
        ByteBuffer y = null, u = null, v = null;
        int newW = 0, newH = 0;
        frameLock.lock();
        try {
            if (frameAvailable && yData != null && uData != null && vData != null) {
                y = yData;
                u = uData;
                v = vData;
                y.position(0); u.position(0); v.position(0);
                newW = frameWidth; newH = frameHeight;
                frameAvailable = false;
                upload = true;
            }
        } finally {
            frameLock.unlock();
        }

        if (!texturesInitialized && !upload) {
            return;
        }

        int drawW = upload ? newW : frameWidth;
        int drawH = upload ? newH : frameHeight;
        if (drawW <= 0 || drawH <= 0) {
            return;
        }

        // Update geometry to preserve aspect ratio (fit center)
        updateScaledPositions(drawW, drawH);

        // Upload/update YUV textures
        int chromaW = drawW / 2;
        int chromaH = drawH / 2;

        if (!texturesInitialized) {
            // First allocation requires valid buffers
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texY);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, drawW, drawH, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texU);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, chromaW, chromaH, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texV);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, chromaW, chromaH, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v);

            texturesInitialized = true;

        } else if (upload) {
            // Update textures with latest frame
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texY);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, drawW, drawH, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texU);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, chromaW, chromaH, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texV);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, chromaW, chromaH, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v);
        }

        renderToFBO(drawW, drawH);
        drawTextureToScreen();
        listener.onDrawFrame(gl);

        if (callInProgress && webRTCClient != null && renderedTexture > 0) {
            sendRenderedFrameToWebRTC(drawW, drawH);
        }
    }

    private void renderToFBO(int drawW, int drawH) {
        if (renderedFbo == 0 || renderedTextureWidth != drawW || renderedTextureHeight != drawH) {
            if (renderedTexture > 0) {
                GLES20.glDeleteTextures(1, new int[]{renderedTexture}, 0);
                renderedTexture = 0;
            }
            if (renderedFbo > 0) {
                GLES20.glDeleteFramebuffers(1, new int[]{renderedFbo}, 0);
                renderedFbo = 0;
            }

            int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            renderedTexture = tex[0];
            setupRGBATexture(renderedTexture, drawW, drawH);

            int[] fbo = new int[1];
            GLES20.glGenFramebuffers(1, fbo, 0);
            renderedFbo = fbo[0];

            renderedTextureWidth = drawW;
            renderedTextureHeight = drawH;
        }

        // Save previous GL state
        int[] prevViewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, prevViewport, 0);
        int[] prevFbo = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, prevFbo, 0);

        try {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderedFbo);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, renderedTexture, 0);

            // Check FBO status
            int fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "FBO not complete: " + fboStatus);
                return;
            }

            GLES20.glViewport(0, 0, drawW, drawH);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            GLES20.glUseProgram(program);
            
            // Bind YUV textures
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texY);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texU);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texV);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            
            GLES20.glUniform1i(uYLoc, 0);
            GLES20.glUniform1i(uULoc, 1);
            GLES20.glUniform1i(uVLoc, 2);

            FloatBuffer mat = ByteBuffer.allocateDirect(9 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mat.put(texTransform).position(0);
            GLES20.glUniformMatrix3fv(uTexTransformLoc, 1, false, mat);

            GLES20.glEnableVertexAttribArray(aPositionLoc);
            GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, positionBuffer);
            GLES20.glEnableVertexAttribArray(aTexCoordLoc);
            GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCordBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            for (Overlay overlay : Overlay.overlayArray) {
                if(overlay.enabled)
                    overlay.draw();
            }

            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glFinish(); // Ensure FBO rendering is complete before using the texture

        } finally {
            GLES20.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, prevFbo[0]);
        }
    }

    private void drawTextureToScreen() {
        if (renderedTexture <= 0) return;

        int prevProgram = getCurrentProgram();
        int[] prevViewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, prevViewport, 0);
        int[] prevFbo = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, prevFbo, 0);

        try {
            // Ensure we're not bound to the FBO when reading the texture
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            GLES20.glUseProgram(textureProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderedTexture);
            GLES20.glUniform1i(textureUTextureLoc, 0);

            GLES20.glEnableVertexAttribArray(textureAPositionLoc);
            GLES20.glVertexAttribPointer(textureAPositionLoc, 3, GLES20.GL_FLOAT, false, 0, positionBuffer);
            GLES20.glEnableVertexAttribArray(textureATexCoordLoc);
            GLES20.glVertexAttribPointer(textureATexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, flippedTexCoordBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(textureAPositionLoc);
            GLES20.glDisableVertexAttribArray(textureATexCoordLoc);
        } finally {
            GLES20.glUseProgram(prevProgram);
            GLES20.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, prevFbo[0]);
        }
    }

    private void sendRenderedFrameToWebRTC(int drawW, int drawH) {
        if (renderedTexture <= 0) return;

        int[] prevViewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, prevViewport, 0);
        int[] prevFbo = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, prevFbo, 0);
        int prevProgram = getCurrentProgram();
        int prevActive = getActiveTexture();
        int prevTex2D = getBoundTexture2D();

        int copiedTex = -1;
        int tempFbo = 0;

        try {
            int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            copiedTex = tex[0];
            setupRGBATexture(copiedTex, drawW, drawH);

            int[] fbo = new int[1];
            GLES20.glGenFramebuffers(1, fbo, 0);
            tempFbo = fbo[0];

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, tempFbo);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, copiedTex, 0);

            if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "Copy FBO not complete");
                GLES20.glDeleteTextures(1, new int[]{copiedTex}, 0);
                return;
            }

            GLES20.glViewport(0, 0, drawW, drawH);
            GLES20.glUseProgram(textureProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderedTexture);
            GLES20.glUniform1i(textureUTextureLoc, 0);

            GLES20.glEnableVertexAttribArray(textureAPositionLoc);
            GLES20.glVertexAttribPointer(textureAPositionLoc, 3, GLES20.GL_FLOAT, false, 0, positionBuffer);
            GLES20.glEnableVertexAttribArray(textureATexCoordLoc);
            GLES20.glVertexAttribPointer(textureATexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, flippedTexCoordBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glFinish(); // Ensure copy is complete before using the texture

            GLES20.glDisableVertexAttribArray(textureAPositionLoc);
            GLES20.glDisableVertexAttribArray(textureATexCoordLoc);

            if (eglHandler == null && webRTCClient.surfaceTextureHelper != null) {
                eglHandler = webRTCClient.surfaceTextureHelper.getHandler();
            }
            if (eglHandler != null && copiedTex > 0) {
                int finalCopiedTex = copiedTex;
                TextureBufferImpl tbuf = new TextureBufferImpl(
                        drawW, drawH, VideoFrame.TextureBuffer.Type.RGB, copiedTex,
                        new Matrix(), eglHandler, yuvConverter,
                        () -> GLES20.glDeleteTextures(1, new int[]{finalCopiedTex}, 0)
                );
                VideoFrame.I420Buffer i420 = yuvConverter.convert(tbuf);
                VideoFrame vf = new VideoFrame(i420, currentOrientation,
                        TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime()));
                ((CustomVideoCapturer) webRTCClient.getVideoCapturer()).writeFrame(vf);
                tbuf.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "send rendered frame to WebRTC failed", e);
        } finally {
            GLES20.glUseProgram(prevProgram);
            GLES20.glActiveTexture(prevActive);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, prevTex2D);
            GLES20.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, prevFbo[0]);
            if (tempFbo != 0) {
                GLES20.glDeleteFramebuffers(1, new int[]{tempFbo}, 0);
            }
        }
    }

    private static void setupRGBATexture(int texId, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
    }

    // Helper methods to get GL state
    private int getCurrentProgram() {
        int[] tmp = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, tmp, 0);
        return tmp[0];
    }

    private int getActiveTexture() {
        int[] tmp = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_ACTIVE_TEXTURE, tmp, 0);
        return tmp[0];
    }

    private int getBoundTexture2D() {
        int[] tmp = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, tmp, 0);
        return tmp[0];
    }

    private static void setupLuminanceTexture(int texId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private static int createProgram(String vSrc, String fSrc) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vSrc);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fSrc);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        int[] link = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Link failed: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        return prog;
    }

    private static int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Compile error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static float[] identity3() {
        return new float[]{
                1,0,0,
                0,1,0,
                0,0,1
        };
    }

    private void updateTexTransform() {
        float[] t1 = translate(-0.5f, -0.5f);
        float[] sMirror = mirrorX ? scale(-1f, 1f) : identity3();
        float[] sFlipY = scale(1f, -1f);
        float[] rot = rotate(rotationDegrees);
        float[] t2 = translate(0.5f, 0.5f);

        float[] m = multiply(t2, multiply(sFlipY, multiply(rot, multiply(sMirror, t1))));
        System.arraycopy(m, 0, texTransform, 0, 9);
    }

    private void updateScaledPositions(int imgW, int imgH) {
        if (positionBuffer == null) return;

        if (surfaceWidth > 0 && surfaceHeight > 0 && imgW > 0 && imgH > 0) {
            float viewAspect = (float) surfaceWidth / surfaceHeight;
            float imgAspect = (float) imgW / imgH;

            View parent = (View) surfaceView.getParent();
            int parentW = parent.getWidth();
            int parentH = parent.getHeight();

            if (parentW == 0 || parentH == 0) return;

            float parentAspect = (float) parentW / parentH;

            float scaledWidthPx, scaledHeightPx;

            if (parentAspect > imgAspect) {
                // Parent is wider → image fills height, width scaled down
                scaledHeightPx = parentH;
                scaledWidthPx = parentH * imgAspect;
            } else {
                // Parent is taller → image fills width, height scaled down
                scaledWidthPx = parentW;
                scaledHeightPx = parentW / imgAspect;
            }
            if(isScaledHeightSet){
                surfaceView.post(() -> {
                    ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                    if (lp.width != (int) scaledWidthPx || lp.height != (int) scaledHeightPx) {
                        lp.width = (int) scaledWidthPx;
                        lp.height = (int) scaledHeightPx;
                        surfaceView.setLayoutParams(lp);
                        for (Overlay overlay: Overlay.overlayArray) {
                            //overlay.updateRendererSize((int)scaledWidthPx,(int)scaledHeightPx);
                        }
                    }
                });

                Overlay.rendererWidth = (int) scaledWidthPx;
                Overlay.rendererHeight = (int) scaledHeightPx;
                listener.onSurfaceInitialized();
                isScaledHeightSet = false;
            }
        }
    }
    static boolean isScaledHeightSet =true;

    private static float[] translate(float tx, float ty) {
        return new float[]{
                1, 0, tx,
                0, 1, ty,
                0, 0, 1
        };
    }

    private static float[] scale(float sx, float sy) {
        return new float[]{
                sx, 0, 0,
                0, sy, 0,
                0, 0, 1
        };
    }

    private static float[] rotate(int deg) {
        double r = Math.toRadians(((deg % 360) + 360) % 360);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        return new float[]{
                c, -s, 0,
                s,  c, 0,
                0,  0, 1
        };
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] m = new float[9];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                m[r*3 + c] = a[r*3] * b[c] + a[r*3 + 1] * b[c + 3] + a[r*3 + 2] * b[c + 6];
            }
        }
        return m;
    }

}
