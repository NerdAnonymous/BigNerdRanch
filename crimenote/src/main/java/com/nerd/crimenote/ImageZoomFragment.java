package com.nerd.crimenote;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * 大图浏览
 */
public class ImageZoomFragment extends Fragment {

    private static final String AGR_IMAGE_PATH = "image_path";

    public static ImageZoomFragment newInstance(String imagePath) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(AGR_IMAGE_PATH, imagePath);

        ImageZoomFragment fragment = new ImageZoomFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String imageFile = (String) getArguments().getSerializable(AGR_IMAGE_PATH);
        Bitmap bitmap = PictureUtils.getScaledBitmap(imageFile, getActivity());

        View view = inflater.inflate(R.layout.fragment_image_zoom, container, false);
        ImageView zoomImageView = view.findViewById(R.id.image_zoom);
        zoomImageView.setImageBitmap(bitmap);

        return view;
    }
}
