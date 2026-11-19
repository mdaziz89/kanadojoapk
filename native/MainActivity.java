package com.kanadojo.app;

import android.os.Bundle;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private TTSBridge tts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TTSBridge(this);
        final WebView wv = getBridge().getWebView();
        wv.addJavascriptInterface(tts, "AndroidTTS");
        // Injected objects only appear on the NEXT page load, and Capacitor may
        // already have started loading, so reload once to make AndroidTTS visible.
        wv.post(new Runnable() {
            @Override public void run() { wv.reload(); }
        });
    }

    @Override
    public void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
