package com.nerdanonymous.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {

    private PhotoPageFragment mPhotoPageFragment;

    public static Intent newIntent(Context context, Uri photoUri) {
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(photoUri);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        mPhotoPageFragment = PhotoPageFragment.newInstance(getIntent().getData());
        return mPhotoPageFragment;
    }

    @Override
    public void onBackPressed() {
        if (mPhotoPageFragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
