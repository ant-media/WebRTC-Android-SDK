package io.antmedia.webrtcandroidframework.canvas;

import androidx.camera.lifecycle.ProcessCameraProvider;

public interface CameraProviderCallback {
    void onReady(ProcessCameraProvider provider);
}