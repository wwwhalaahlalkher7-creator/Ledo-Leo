package com.smarty.sunny;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * High-Performance SoundPool Game Audio Manager for Ludo King.
 * Provides zero-latency, instant sound effects for token steps, dice rolls,
 * kills, clicks, game starts, and wins without UI thread lag.
 */
public class AudioManager {
    private static final String TAG = "AudioManager";
    private static AudioManager instance;

    private Context context;
    private boolean isSoundEnabled = true;
    private SoundPool soundPool;
    private final Map<Integer, Integer> soundMap = new HashMap<>();
    private boolean isLoaded = false;

    private AudioManager() {}

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public synchronized void init(Context ctx) {
        if (ctx == null) return;
        this.context = ctx.getApplicationContext();

        if (soundPool == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(10)
                        .setAudioAttributes(audioAttributes)
                        .build();
            } else {
                soundPool = new SoundPool(10, android.media.AudioManager.STREAM_MUSIC, 0);
            }

            soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
                if (status == 0) {
                    isLoaded = true;
                }
            });

            // Pre-load all game sound effects into memory for instant zero-latency playback
            loadSound(R.raw.step);
            loadSound(R.raw.diceroll);
            loadSound(R.raw.move);
            loadSound(R.raw.click);
            loadSound(R.raw.safe);
            loadSound(R.raw.panta);
            loadSound(R.raw.death);
            loadSound(R.raw.gamestartsound);
            loadSound(R.raw.congratulations);
            loadSound(R.raw.boing);
            loadSound(R.raw.dice);
            loadSound(R.raw.home_reach);
        }
    }

    private void loadSound(int resId) {
        if (context == null || soundPool == null) return;
        try {
            int soundId = soundPool.load(context, resId, 1);
            soundMap.put(resId, soundId);
        } catch (Exception e) {
            Log.e(TAG, "Error loading sound: " + resId + " - " + e.getMessage());
        }
    }

    public void setSoundEnabled(boolean enabled) {
        this.isSoundEnabled = enabled;
        if (!enabled) {
            stopAll();
        }
    }

    public boolean isSoundEnabled() {
        return isSoundEnabled;
    }

    public void setCustomAudioEnabled(boolean enabled) {}
    public boolean isCustomAudioEnabled() { return false; }

    // ==========================================
    // ZERO-LATENCY SOUND EFFECTS
    // ==========================================

    public void playSound(final int resId) {
        if (!isSoundEnabled || soundPool == null) return;

        Integer soundId = soundMap.get(resId);
        if (soundId != null && soundId > 0) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        } else if (context != null) {
            // Lazy load if not yet in map
            try {
                int newSoundId = soundPool.load(context, resId, 1);
                soundMap.put(resId, newSoundId);
                soundPool.play(newSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (Exception ignored) {}
        }
    }

    public void playNormalSound(final int resId) {
        playSound(resId);
    }

    // Individual Game Sounds
    public void playStepSound() {
        playSound(R.raw.step);
    }

    public void playDiceSound() {
        playSound(R.raw.diceroll);
    }

    public void playMoveSound() {
        playSound(R.raw.step); // Goti chalte time step sound effect
    }

    public void playSafeSound() {
        playSound(R.raw.safe);
    }

    public void playHomeReachSound() {
        playSound(R.raw.home_reach);
    }

    public void playClickSound() {
        playSound(R.raw.click);
    }

    public void playGameStartSound() {
        playSound(R.raw.gamestartsound);
    }

    public void playDeathSound() {
        playSound(R.raw.death);
    }

    public void playTokenCaptureSound() {
        playDeathSound();
    }

    public void playEnterHomePathSound() {
        playSound(R.raw.panta);
    }

    public void playWinSound() {
        playSound(R.raw.congratulations);
    }

    public void playCustomAudio(final int resId) {
        playSound(resId);
    }

    public void stopCustomChannelOnly() {
        stopAll();
    }

    public void stopAll() {
        if (soundPool != null) {
            try {
                soundPool.autoPause();
            } catch (Exception ignored) {}
        }
    }

    public void release() {
        stopAll();
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {}
            soundPool = null;
        }
        soundMap.clear();
        context = null;
    }
}
