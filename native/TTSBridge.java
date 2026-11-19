package com.kanadojo.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import java.util.Locale;

/** Native Android text-to-speech exposed to the web page as window.AndroidTTS. */
public class TTSBridge {
    private static final String GOOGLE = "com.google.android.tts";
    private final Context ctx;
    private TextToSpeech tts;
    private volatile String state = "loading"; // loading | ok | nojapanese | initfailed
    private volatile String engine = "default";
    private volatile String lastError = "";
    private volatile String pending = null;
    private boolean triedGoogle = false;

    public TTSBridge(Context context) {
        ctx = context.getApplicationContext();
        start(null);
    }

    private void start(final String engineName) {
        state = "loading";
        engine = engineName == null ? "default" : engineName;
        final TextToSpeech[] holder = new TextToSpeech[1];
        TextToSpeech.OnInitListener l = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) { onReady(holder[0], status); }
        };
        try {
            holder[0] = engineName == null ? new TextToSpeech(ctx, l) : new TextToSpeech(ctx, l, engineName);
            tts = holder[0];
        } catch (Exception e) {
            lastError = "create: " + e;
            state = "initfailed";
        }
    }

    private void onReady(TextToSpeech t, int status) {
        if (t == null) t = tts;
        if (status != TextToSpeech.SUCCESS || t == null) {
            lastError = "init status " + status;
            if (!triedGoogle) { triedGoogle = true; start(GOOGLE); return; }
            state = "initfailed";
            return;
        }
        int r = t.setLanguage(Locale.JAPAN);
        boolean ja = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED);
        if (!ja && !triedGoogle) {
            // default engine (e.g. Samsung) has no Japanese: try the Google engine
            triedGoogle = true;
            try { t.shutdown(); } catch (Exception ignored) {}
            start(GOOGLE);
            return;
        }
        if (!ja) { state = "nojapanese"; return; }
        try {
            t.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        } catch (Exception ignored) {}
        t.setSpeechRate(0.8f);
        t.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) {}
            @Override public void onError(String id) { lastError = "speak error"; }
        });
        try { engine = t.getDefaultEngine(); } catch (Exception ignored) {}
        state = "ok";
        if (pending != null) { String p = pending; pending = null; speakNow(p); }
    }

    private void speakNow(String text) {
        try {
            int r = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kana");
            if (r != TextToSpeech.SUCCESS) lastError = "speak returned " + r;
        } catch (Exception e) { lastError = "speak: " + e; }
    }

    @JavascriptInterface
    public void speak(String text) {
        if (state.equals("ok")) speakNow(text);
        else if (state.equals("loading")) pending = text;
    }

    @JavascriptInterface
    public String status() { return state; }

    @JavascriptInterface
    public String info() {
        return "state=" + state + ", engine=" + engine + (lastError.isEmpty() ? "" : ", err=" + lastError);
    }

    public void shutdown() {
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
    }
}
