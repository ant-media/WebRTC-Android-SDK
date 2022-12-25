package io.antmedia.webrtcandroidframework;

import static org.mockito.Mockito.when;

import android.media.MediaCodecInfo;
import android.os.Build;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.VideoCodecMimeType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class HardwareVideoEncoderFactoryTest extends TestCase {

    private static final String GOOGLE_H264_HW_ENCODER = "OMX.google.h264.encoder";

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    @Test
    public void testIsHardwareSupportedInCurrentSdk() throws Exception {
        HardwareVideoEncoderFactory encoderFactory = new HardwareVideoEncoderFactory(null, true, true);
        MediaCodecInfo info = Mockito.mock(MediaCodecInfo.class);
        VideoCodecMimeType videoCodecMimeType = VideoCodecMimeType.H264;
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 2046);

        when(info.getName()).thenReturn(GOOGLE_H264_HW_ENCODER);
        when(info.isHardwareAccelerated()).thenReturn(false);

        assertTrue(encoderFactory.isHardwareSupportedInCurrentSdk(info, videoCodecMimeType));
    }

    //Test that the HW encoder is working on google's hw encoder.
    @Test
    public void testIsHardwareSupportedInCurrentSdkH264() {
        HardwareVideoEncoderFactory encoderFactory = new HardwareVideoEncoderFactory(null, true, true);
        MediaCodecInfo info = Mockito.mock(MediaCodecInfo.class);

        when(info.getName()).thenReturn(GOOGLE_H264_HW_ENCODER);
        //Following method should return true in all chipsets so it then support huawei devices etc.
        assertTrue(encoderFactory.isHardwareSupportedInCurrentSdkH264(info));

        //Make this field public and empty. Working with less control is better than not working
        assertTrue(HardwareVideoEncoderFactory.H264_HW_EXCEPTION_MODELS.isEmpty());
    }
}