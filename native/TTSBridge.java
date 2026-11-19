package com.kanadojo.app;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import java.util.Locale;

/** Native Android text-to-speech exposed to the web page as window.AndroidTTS. */
public class TTSBridge implements TextToSpeech.OnInitListener {
    private final TextToSpeech tts;
    private volatile boolean ready = false;
    private volatile boolean japanese = false;
    private volatile String pending = null;

    public TTSBridge(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        int r = tts.setLanguage(Locale.JAPAN);
        japanese = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED);
        tts.setSpeechRate(0.8f);
        ready = true;
        if (pending != null) {
            String p = pending;
            pending = null;
            speakNow(p);
        }
    }

    private void speakNow(String text) {
        if (!japanese) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kana");
    }

    @JavascriptInterface
    public void speak(String text) {
        if (!ready) { pending = text; return; }
        speakNow(text);
    }

    /** "ok", "loading" or "nojapanese". */
    @JavascriptInterface
    public String status() {
        if (!ready) return "loading";
        return japanese ? "ok" : "nojapanese";
    }

    public void shutdown() {
        try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {}
    }
}
