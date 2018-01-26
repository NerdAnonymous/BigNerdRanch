package com.nerd.criminalintent;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * 大图浏览
 */
public class ImageZoomDialog extends DialogFragment {

    private static final String AGR_IMAGE_PATH = "image_path";

    public static ImageZoomDialog newInstance(String imagePath) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(AGR_IMAGE_PATH, imagePath);

        ImageZoomDialog dialog = new ImageZoomDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_DialogWhenLarge_NoActionBar);
        } else {
            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_DeviceDefault_DialogWhenLarge_NoActionBar);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String imageFile = (String) getArguments().getSerializable(AGR_IMAGE_PATH);
        Bitmap bitmap = PictureUtils.getScaledBitmap(imageFile, getActivity());

        View view = inflater.inflate(R.layout.dialog_image_zoom, container, false);
        ImageView zoomImageView = view.findViewById(R.id.image_zoom);
        zoomImageView.setImageBitmap(bitmap);

        return view;
    }
}
