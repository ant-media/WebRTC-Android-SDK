package io.antmedia.webrtcandroidframework.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionsHandler {
    public interface PermissionCallback {
        void onPermissionResult();
    }
    private final Activity activity;
    private PermissionCallback permissionCallback;

    public static final String[] REQUIRED_EXTENDED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
            new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT}
            :
            new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    // List of mandatory application permissions.
    public static final String[] REQUIRED_MINIMUM_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.INTERNET"};

    public PermissionsHandler(Activity activity) {
        this.activity = activity;
    }

    public boolean checkAndRequestPermisssions(boolean isExtended, boolean requestBluetoothForPlay, PermissionCallback permissionCallback) {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        if(isExtended) {
            permissions.addAll(Arrays.asList(REQUIRED_EXTENDED_PERMISSIONS));
        }

        if(requestBluetoothForPlay){
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (hasPermissions(activity.getApplicationContext(), permissions)) {
            return true;
        }
        else {
            this.permissionCallback = permissionCallback;
            showPermissionsErrorAndRequest(permissions);
            return false;
        }
    }

    public boolean hasPermissions(Context context, List<String> permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(PermissionsHandler.class.getSimpleName(), "Permission required:"+permission);
                    return false;
                }
            }
        }
        return true;
    }
    public void showPermissionsErrorAndRequest(List<String> permissions) {
        makeToast("You need permissions before", Toast.LENGTH_SHORT);
        String[] permissionArray = new String[permissions.size()];
        permissions.toArray(permissionArray);
        ActivityCompat.requestPermissions(activity, permissionArray, 1);
    }

    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        permissionCallback.onPermissionResult();
    }

    public void makeToast(String messageText, int lengthLong) {
        activity.runOnUiThread(() -> Toast.makeText(activity, messageText, lengthLong).show());
    }

}

