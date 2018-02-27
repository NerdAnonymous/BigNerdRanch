package com.nerdanonymous.sunset;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class SunsetFragment extends Fragment {

    private View mSceneView;
    private View mSkyView;
    private ImageView mSunView;

    private int mBlueSkyColor;
    private int mSunsetSkyColor;
    private int mNightSkyColor;
    private int mBrightSunColor;
    private int mHotSunColor;

    private SUN_STATE mSunState = SUN_STATE.SUNSET;

    private AnimatorSet sunsetAnimator;
    private AnimatorSet sunriseAnimator;

    public static SunsetFragment newInstance() {
        return new SunsetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sunset, container, false);

        mSceneView = view;
        mSkyView = view.findViewById(R.id.sky);
        mSunView = view.findViewById(R.id.sun);

        Resources resources = getResources();
        mBlueSkyColor = resources.getColor(R.color.blue_sky);
        mSunsetSkyColor = resources.getColor(R.color.sunset_sky);
        mNightSkyColor = resources.getColor(R.color.night_sky);
        mBrightSunColor = resources.getColor(R.color.bright_sun);
        mHotSunColor = resources.getColor(R.color.hot_sun);

        initPulseAnimator();

        mSceneView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSunAnimation();
            }
        });

        return view;
    }

    private void initPulseAnimator() {
        ObjectAnimator pulseAnimator = ObjectAnimator
                .ofPropertyValuesHolder(mSunView,
                        PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.1f))
                .setDuration(600);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        final GradientDrawable drawable = (GradientDrawable) mSunView.getDrawable();

        if (null == drawable) {
            return;
        }

        ValueAnimator colorAnimator=ValueAnimator
                .ofInt(mBrightSunColor, mHotSunColor)
                .setDuration(600);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        colorAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                drawable.setColor((int) animation.getAnimatedValue());
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(pulseAnimator).with(colorAnimator);
        animatorSet.start();
    }

    private void startSunAnimation() {
        float sunYStart = mSunView.getTop();
        float sunYEnd = mSkyView.getHeight() + 0.1f * mSunView.getHeight();

        if (null == sunsetAnimator) {
            ObjectAnimator sunsetHeightAnimator = ObjectAnimator
                    .ofFloat(mSunView, "y", sunYStart, sunYEnd)
                    .setDuration(3000);
            sunsetHeightAnimator.setInterpolator(new AccelerateInterpolator());
            sunsetHeightAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mSunState = SUN_STATE.SUNSET;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mSunState = SUN_STATE.SUNRISE;
                }
            });

            ObjectAnimator sunsetSkyAnimation = ObjectAnimator
                    .ofInt(mSkyView, "backgroundColor", mBlueSkyColor, mSunsetSkyColor)
                    .setDuration(3000);
            sunsetSkyAnimation.setEvaluator(new ArgbEvaluator());

            ObjectAnimator sunsetNightSkyAnimator = ObjectAnimator
                    .ofInt(mSkyView, "backgroundColor", mSunsetSkyColor, mNightSkyColor)
                    .setDuration(1500);
            sunsetNightSkyAnimator.setEvaluator(new ArgbEvaluator());

            sunsetAnimator = new AnimatorSet();
            sunsetAnimator
                    .play(sunsetHeightAnimator)
                    .with(sunsetSkyAnimation)
                    .before(sunsetNightSkyAnimator);
        }

        if (null == sunriseAnimator) {
            ObjectAnimator sunriseHeightAnimator = ObjectAnimator
                    .ofFloat(mSunView, "y", sunYEnd, sunYStart)
                    .setDuration(3000);
            sunriseHeightAnimator.setInterpolator(new DecelerateInterpolator());
            sunriseHeightAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mSunState = SUN_STATE.SUNRISE;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mSunState = SUN_STATE.SUNSET;
                }
            });

            ObjectAnimator sunriseSkyAnimation = ObjectAnimator
                    .ofInt(mSkyView, "backgroundColor", mSunsetSkyColor, mBlueSkyColor)
                    .setDuration(3000);
            sunriseSkyAnimation.setEvaluator(new ArgbEvaluator());

            ObjectAnimator sunriseNightSkyAnimator = ObjectAnimator
                    .ofInt(mSkyView, "backgroundColor", mNightSkyColor, mSunsetSkyColor)
                    .setDuration(1500);
            sunriseNightSkyAnimator.setEvaluator(new ArgbEvaluator());

            sunriseAnimator = new AnimatorSet();
            sunriseAnimator
                    .play(sunriseHeightAnimator)
                    .with(sunriseSkyAnimation)
                    .after(sunriseNightSkyAnimator);
        }

        if (mSunState == SUN_STATE.SUNSET) {
            if (!sunsetAnimator.isRunning()) {
                sunsetAnimator.start();
            }
        } else {
            if (!sunriseAnimator.isRunning()) {
                sunriseAnimator.start();
            }
        }
    }
}
