package io.antmedia.webrtcandroidframework.core;

import android.media.MediaCodecInfo;

import org.webrtc.EglBase;
import org.webrtc.HardwareVideoEncoderFactory;

public class AMSHardwareVideoEncoderFactory extends HardwareVideoEncoderFactory {
    public AMSHardwareVideoEncoderFactory(EglBase.Context sharedContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        super(sharedContext, enableIntelVp8Encoder, enableH264HighProfile);
    }

    public boolean isHardwareSupportedInCurrentSdkH264(MediaCodecInfo info) {
        // First, H264 hardware might perform poorly on this model.
        String name = info.getName();
        // QCOM and Exynos H264 encoders are always supported.
        //return name.startsWith(QCOM_PREFIX) || name.startsWith(EXYNOS_PREFIX);
        //ignore chipsets and return true for all of them
        return true;
    }
}
