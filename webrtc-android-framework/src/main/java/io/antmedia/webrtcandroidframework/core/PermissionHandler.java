package io.antmedia.webrtcandroidframework.core;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionHandler {

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 0;
    public static final int PUBLISH_PERMISSION_REQUEST_CODE = 1;
    public static final int PLAY_PERMISSION_REQUEST_CODE = 2;


    public static final String[] BLUETOOTH_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
            new String[] {Manifest.permission.BLUETOOTH_CONNECT} : new String[] {} ;

    public static final String[] CAMERA_PERMISSIONS =
            new String[] {Manifest.permission.CAMERA};

    public static final String[] PUBLISH_PERMISSIONS =
            new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    public static final String[] REQUIRED_MINIMUM_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.INTERNET"};

    public static final String[] FULL_PERMISSIONS = concatArrays(
            BLUETOOTH_PERMISSIONS,
            CAMERA_PERMISSIONS,
            PUBLISH_PERMISSIONS,
            REQUIRED_MINIMUM_PERMISSIONS
    );

    private static String[] concatArrays(String[]... arrays) {
        int totalLength = 0;
        for (String[] array : arrays) {
            totalLength += array.length;
        }
        String[] result = new String[totalLength];
        int index = 0;
        for (String[] array : arrays) {
            for (String element : array) {
                result[index++] = element;
            }
        }
        return result;
    }

    public static boolean checkCameraPermissions(Activity activity){
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        permissions.addAll(Arrays.asList(CAMERA_PERMISSIONS));
        return hasPermissions(activity, permissions);
    }

    public static boolean checkPublishPermissions(Activity activity, boolean bluetoothEnabled){
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        permissions.addAll(Arrays.asList(PUBLISH_PERMISSIONS));

        if(bluetoothEnabled){
            permissions.addAll(Arrays.asList(BLUETOOTH_PERMISSIONS));
        }

        return hasPermissions(activity, permissions);
    }

    public static boolean checkPlayPermissions(Activity activity, boolean bluetoothEnabled){
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        if(bluetoothEnabled){
            permissions.addAll(Arrays.asList(BLUETOOTH_PERMISSIONS));
        }
        return hasPermissions(activity,permissions);
    }

    public static void requestPermissions(Activity activity, List<String> permissions, int requestCode) {
        String[] permissionArray = new String[permissions.size()];
        permissions.toArray(permissionArray);
        ActivityCompat.requestPermissions(activity, permissionArray, requestCode);
    }

    public static boolean hasPermissions(Activity activity, List<String> permissions) {
        if (activity != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(PermissionHandler.class.getSimpleName(), "Permission required:"+permission);
                    return false;
                }
            }
        }
        return true;
    }

    public static void requestCameraPermissions(Activity activity){
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        permissions.addAll(Arrays.asList(CAMERA_PERMISSIONS));
        requestPermissions(activity, permissions, CAMERA_PERMISSION_REQUEST_CODE);
    }

    public static void requestPublishPermissions(Activity activity, boolean bluetoothEnabled){
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        permissions.addAll(Arrays.asList(PUBLISH_PERMISSIONS));

        if(bluetoothEnabled){
            permissions.addAll(Arrays.asList(BLUETOOTH_PERMISSIONS));
        }
        requestPermissions(activity, permissions, PUBLISH_PERMISSION_REQUEST_CODE);
    }

    public static void requestPlayPermissions(Activity activity, boolean bluetoothEnabled){
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        if(bluetoothEnabled){
            permissions.addAll(Arrays.asList(BLUETOOTH_PERMISSIONS));
        }
        requestPermissions(activity, permissions, PLAY_PERMISSION_REQUEST_CODE);
    }
}

