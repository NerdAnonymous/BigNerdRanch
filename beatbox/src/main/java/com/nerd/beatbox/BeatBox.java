package com.nerd.beatbox;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BeatBox {

    private static final String TAG = "BeatBox";

    private static final String SOUND_FOLDER = "sample_sounds";
    private static final int MAX_SOUNDS = 5;

    private AssetManager mAssets;
    private List<Sound> mSounds = new ArrayList<>();
    private SoundPool mSoundPool;

    @SuppressWarnings("deprecation")
    public BeatBox(Context context) {
        mAssets = context.getAssets();
        mSoundPool = new SoundPool(MAX_SOUNDS, AudioManager.STREAM_MUSIC, 0);
        loadSounds();
    }

    private void loadSounds() {
        String[] soundNames = null;
        try {
            soundNames = mAssets.list(SOUND_FOLDER);
        } catch (Exception e) {
            Log.e(TAG, "Could not list assets", e);
        }

        if (null != soundNames) {
            String assetPath;
            Sound sound;
            for (String fileName : soundNames) {
                try {
                    assetPath = SOUND_FOLDER + File.separator + fileName;
                    sound = new Sound(assetPath);
                    load(sound);
                    mSounds.add(sound);
                } catch (Exception e) {
                    Log.e(TAG, "Could not load sound " + fileName, e);
                }
            }
        }
    }

    public List<Sound> getSounds() {
        return mSounds;
    }

    private void load(Sound sound) throws IOException {
        AssetFileDescriptor afd = mAssets.openFd(sound.getAssetPath());
        int soundId = mSoundPool.load(afd, 1);
        sound.setSoundId(soundId);
    }

    public void play(Sound sound) {
        Integer soundId = sound.getSoundId();
        if (null == soundId) {
            return;
        }
        mSoundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void release(){
        mSoundPool.release();
    }
}
