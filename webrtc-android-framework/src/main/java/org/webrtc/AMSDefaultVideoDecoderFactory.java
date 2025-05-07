package org.webrtc;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class AMSDefaultVideoDecoderFactory implements VideoDecoderFactory {
    private final VideoDecoderFactory hardwareVideoDecoderFactory;
    private final VideoDecoderFactory softwareVideoDecoderFactory = new SoftwareVideoDecoderFactory();
    private final @Nullable VideoDecoderFactory platformSoftwareVideoDecoderFactory;

    /**
     * Create decoder factory using default hardware decoder factory.
     */
    public AMSDefaultVideoDecoderFactory(@Nullable EglBase.Context eglContext) {
        this.hardwareVideoDecoderFactory = new HardwareVideoDecoderFactory(eglContext);
        this.platformSoftwareVideoDecoderFactory = new PlatformSoftwareVideoDecoderFactory(eglContext);
    }

    /**
     * Create decoder factory using explicit hardware decoder factory.
     */
    AMSDefaultVideoDecoderFactory(VideoDecoderFactory hardwareVideoDecoderFactory) {
        this.hardwareVideoDecoderFactory = hardwareVideoDecoderFactory;
        this.platformSoftwareVideoDecoderFactory = null;
    }

    @Override
    public @Nullable VideoDecoder createDecoder(VideoCodecInfo codecType) {
        VideoDecoder softwareDecoder = softwareVideoDecoderFactory.createDecoder(codecType);
        final VideoDecoder hardwareDecoder = hardwareVideoDecoderFactory.createDecoder(codecType);
        if (softwareDecoder == null && platformSoftwareVideoDecoderFactory != null) {
            softwareDecoder = platformSoftwareVideoDecoderFactory.createDecoder(codecType);
        }
        if (hardwareDecoder != null && softwareDecoder != null) {
            // Both hardware and software supported, wrap it in a software fallback
            return new VideoDecoderFallback(
                    /* fallback= */ softwareDecoder, /* primary= */ hardwareDecoder);
        }
        return hardwareDecoder != null ? hardwareDecoder : softwareDecoder;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        LinkedHashSet<VideoCodecInfo> supportedCodecInfos = new LinkedHashSet<VideoCodecInfo>();

        supportedCodecInfos.addAll(Arrays.asList(softwareVideoDecoderFactory.getSupportedCodecs()));
        supportedCodecInfos.addAll(Arrays.asList(hardwareVideoDecoderFactory.getSupportedCodecs()));
        if (platformSoftwareVideoDecoderFactory != null) {
            supportedCodecInfos.addAll(
                    Arrays.asList(platformSoftwareVideoDecoderFactory.getSupportedCodecs()));
        }

        return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }
}