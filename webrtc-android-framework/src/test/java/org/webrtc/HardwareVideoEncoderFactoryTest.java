package org.webrtc;

import android.media.MediaCodecInfo;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;

public class HardwareVideoEncoderFactoryTest extends TestCase {
    private static final String GOOGLE_H264_HW_ENCODER = "OMX.google.h264.encoder";

    @Test
    public void testIsHardwareSupportedInCurrentSdkH264() {
        HardwareVideoEncoderFactory encoderFactory = Mockito.mock(HardwareVideoEncoderFactory.class);
        MediaCodecInfo info = Mockito.mock(MediaCodecInfo.class);
        Mockito.when(encoderFactory.findCodecForType(VideoCodecMimeType.H264))
                .thenReturn(info);

        Mockito.when(info.getName()).thenReturn(GOOGLE_H264_HW_ENCODER);

        assertTrue(encoderFactory.isHardwareSupportedInCurrentSdkH264(info));
    }
}