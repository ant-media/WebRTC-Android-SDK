In building native codes, we apply the H265 related patches to the Android M79 checkout and build it 
for arm, arm64, x86 and x64 architectures

# Current commits for H265 are as follows

- Author: mekya <ahmetmermerkaya@gmail.com>
  Date:   Mon Apr 13 13:37:39 2020 +0300
  
  Support hevc IDR nal unit used by GPU
  
- Author: mekya <ahmetmermerkaya@gmail.com>
  Date:   Mon Mar 23 09:13:58 2020 +0300
  
      h265 support for sending and receiving

# args.gn in native library build is like this

target_os = "android"
target_cpu = "arm"   #change this value to arm64, x64, x86 to build for different architectures
ffmpeg_branding="Chrome"
rtc_use_h264=true
rtc_include_tests=false
is_debug=false


# Java source files that needs to be copied from branch:

src/sdk/android/api
src/sdk/android/src
src/examples/androidapp
src/modules/audio_device/android/java/src/org/webrtc/voiceengine
src/rtc_base/java/src/org/webrtc
Generated Java files that needs to be copied:
build_directory/gen/api/priority_enums.srcjar

# Modifications to the Copied source:
- replace all import android.support.annotation.Nullable; with import androidx.annotation.Nullable;
