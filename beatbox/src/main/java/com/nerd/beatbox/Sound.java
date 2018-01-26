package com.nerd.beatbox;

import java.io.File;

public class Sound {

    private String mAssetPath;

    private String mName;

    private Integer mSoundId;

    public Sound(String assetPath) {
        mAssetPath = assetPath;
        String[] components = assetPath.split(File.separator);
        String filName = components[components.length - 1];
        mName = filName.replace(".wav", "");
    }

    public String getAssetPath() {
        return mAssetPath;
    }

    public String getName() {
        return mName;
    }

    public Integer getSoundId() {
        return mSoundId;
    }

    public void setSoundId(Integer soundId) {
        mSoundId = soundId;
    }
}
