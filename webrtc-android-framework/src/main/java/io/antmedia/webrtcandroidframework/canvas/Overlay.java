package io.antmedia.webrtcandroidframework.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class Overlay {
    private int texture = -1;
    private int program = -1;
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;

    private float size = 0.3f;    // relative size (NDC units)
    private float cx = 0f, cy = 0f;
    private float overlayAspect = 1f;
    private float rendererAspect = 1f;

    public static int rendererWidth,rendererHeight;
    public static ArrayList<Overlay> overlayArray = new ArrayList<>();
    public Overlay(Context ctx, int resId, float x, float y) {
        cx = x;
        cy = y;
        rendererAspect = (float) rendererWidth / rendererHeight;

        Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), resId);
        overlayAspect = (float) bmp.getWidth() / bmp.getHeight();

        texture = loadTexture(bmp);
        bmp.recycle();

        initProgram();
        initBuffers();

        Overlay.overlayArray.add(this);
    }

    public Overlay(Context ctx, String text, int textSize, int color, float x, float y) {
        cx = x;
        cy = y;
        rendererAspect = (float) rendererWidth / rendererHeight;

        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(color);
        paint.setAntiAlias(true);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int w = (int) Math.ceil(paint.measureText(text));
        int h = (int) Math.ceil(Math.abs(bounds.top) + Math.abs(bounds.bottom));
        if (w == 0) w = 1;
        if (h == 0) h = 1;
        overlayAspect = (float) w / h;

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.TRANSPARENT);
        c.drawText(text, 0, Math.abs(bounds.top), paint);

        texture = loadTexture(bmp);
        bmp.recycle();

        initProgram();
        initBuffers();
        Overlay.overlayArray.add(this);
    }

    // ------------------------ New Function ------------------------
    /**
     * Set overlay size (in normalized device coordinates).
     * 1.0 = full width of screen, 0.5 = half, etc.
     * Automatically updates vertex buffer.
     */
    public void setSize(float newSize) {
        this.size = newSize;
        updateVertexBuffer();
    }
    // --------------------------------------------------------------

    private void initProgram() {
        String vsh =
                "attribute vec4 aPos;\n" +
                        "attribute vec2 aTex;\n" +
                        "varying vec2 vTex;\n" +
                        "void main() {\n" +
                        "    gl_Position = aPos;\n" +
                        "    vTex = vec2(aTex.x, 1.0 - aTex.y);\n" +
                        "}";

        String fsh =
                "precision mediump float;\n" +
                        "varying vec2 vTex;\n" +
                        "uniform sampler2D tex;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(tex, vTex);\n" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vsh);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fsh);

        // --- Create program ---
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    private void initBuffers() {
        float[] uv = {0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f};
        uvBuffer = ByteBuffer.allocateDirect(uv.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        uvBuffer.put(uv).position(0);
        updateVertexBuffer();
    }

    private void updateVertexBuffer() {
        float halfW = size / 2f;
        float halfH = halfW / overlayAspect * rendererAspect;

        float[] v = {
                cx - halfW, cy + halfH, 0f,
                cx + halfW, cy + halfH, 0f,
                cx - halfW, cy - halfH, 0f,
                cx + halfW, cy - halfH, 0f
        };

        if (vertexBuffer == null)
            vertexBuffer = ByteBuffer.allocateDirect(v.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(v).position(0);
    }

    public void updateRendererSize(int w, int h) {
        rendererAspect = (float) w / h;
        updateVertexBuffer();
    }

    public void draw() {
        if (program <= 0 || texture <= 0) return;

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(program);

        int posLoc = GLES20.glGetAttribLocation(program, "aPos");
        int texLoc = GLES20.glGetAttribLocation(program, "aTex");
        int samplerLoc = GLES20.glGetUniformLocation(program, "tex");

        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 8, uvBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(samplerLoc, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
        GLES20.glUseProgram(0);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadTexture(Bitmap bmp) {
        int[] id = new int[1];
        GLES20.glGenTextures(1, id, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        return id[0];
    }

    private int loadShader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }
}