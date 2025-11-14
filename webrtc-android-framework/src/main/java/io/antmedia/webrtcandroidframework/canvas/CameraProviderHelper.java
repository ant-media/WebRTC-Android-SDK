package io.antmedia.webrtcandroidframework.canvas;

import android.content.Context;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class CameraProviderHelper {
    public final Context context;
    public CameraProviderCallback callback;
    ProcessCameraProvider cameraProvider;
    static int defaultLensFacing = CameraSelector.LENS_FACING_BACK;
    public static int lensFacing = defaultLensFacing;
    public CameraProviderHelper(Context context, CameraProviderCallback callback) {
        this.context = context;
        this.callback = callback;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                 cameraProvider = cameraProviderFuture.get();
                if (callback != null) {
                    callback.onReady(cameraProvider);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void startCamera(ImageAnalysis imageAnalysis) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                (LifecycleOwner) context,
                cameraSelector,
                imageAnalysis
        );
    }
    public void restartCamera(ImageAnalysis imageAnalysis) {
        if (cameraProvider == null) return;
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProvider.bindToLifecycle(
                (LifecycleOwner) context,
                cameraSelector,
                imageAnalysis
        );
    }
    public void switchCamera(ImageAnalysis imageAnalysis) {
        // Toggle between front and back
        synchronized (this) {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(imageAnalysis);
        }
    }
    public void stopCamera(){
         cameraProvider.unbindAll();
    }

}

