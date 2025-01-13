package io.antmedia.webrtc_android_sample_app.basic;


import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;


import io.antmedia.webrtc_android_sample_app.R;

import io.antmedia.webrtcandroidframework.core.PermissionHandler;

/**
 * This sample demonstrates how to publish a stream from an Android WebView.
 *
 * IMPORTANT NOTE: To successfully publish from an Android WebView, you MUST enable
 * the VP8 codec in the Ant Media Server application settings.
 *
 * Without enabling VP8, publishing attempts will result in a publish timeout error.
 */

public class WebviewActivity extends Activity {

    private WebView webView;
    private String publishPageUrl = "https://test.antmedia.io/LiveApp";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);

        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webview);
        setupWebView();
        if(!PermissionHandler.checkPublishPermissions(this, false)){
            PermissionHandler.requestPublishPermissions(this, false);
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    request.grant(request.getResources());
                });
            }
        });

        webView.loadUrl(publishPageUrl);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PermissionHandler.PUBLISH_PERMISSION_REQUEST_CODE){

            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                setupWebView();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
