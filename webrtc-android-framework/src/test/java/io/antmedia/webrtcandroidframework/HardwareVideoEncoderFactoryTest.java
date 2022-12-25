package io.antmedia.webrtcandroidframework;

import android.media.MediaCodecInfo;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;
import org.webrtc.HardwareVideoEncoderFactory;

public class HardwareVideoEncoderFactoryTest extends TestCase {

    private static final String GOOGLE_H264_HW_ENCODER = "OMX.google.h264.encoder";

    //Test that the HW encoder is working on google's hw encoder.
    @Test
    public void testIsHardwareSupportedInCurrentSdkH264() {
        HardwareVideoEncoderFactory encoderFactory = new HardwareVideoEncoderFactory(null, true, true);
        MediaCodecInfo info = Mockito.mock(MediaCodecInfo.class);

        Mockito.when(info.getName()).thenReturn(GOOGLE_H264_HW_ENCODER);
        //Following method should return true in all chipsets so it then support huawei devices etc.
        assertTrue(encoderFactory.isHardwareSupportedInCurrentSdkH264(info));

        //Make this field public and empty. Working with less control is better than not working
        assertTrue(HardwareVideoEncoderFactory.H264_HW_EXCEPTION_MODELS.isEmpty());

        fail("Check if it's failing");
    }
}