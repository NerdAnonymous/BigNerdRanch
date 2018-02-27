package com.nerd.crimenote;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class ImageZoomActivity extends SingleFragmentActivity {

    private static final String EXTRA_IMAGE_PATH = "image_path";

    public static Intent newIntent(Context context, String imagePath) {
        Intent intent = new Intent(context, ImageZoomActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        return ImageZoomFragment.newInstance(getIntent().getStringExtra(EXTRA_IMAGE_PATH));
    }
}
