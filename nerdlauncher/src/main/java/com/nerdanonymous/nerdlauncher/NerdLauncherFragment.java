package com.nerdanonymous.nerdlauncher;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NerdLauncherFragment extends Fragment {

    private static final String TAG = NerdLauncherFragment.class.getSimpleName();

    private RecyclerView mRecyclerView;

    public static NerdLauncherFragment newInstance() {
        return new NerdLauncherFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nerd_launcher, container, false);

        mRecyclerView = view.findViewById(R.id.nerd_launcher_recycle_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        setupAdapter();

        return view;
    }

    private void setupAdapter() {
        Intent startupIntent = new Intent(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        FragmentActivity activity = getActivity();
        if (null != activity) {
            final PackageManager pm = getActivity().getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(startupIntent, 0);

            Collections.sort(activities, new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo o1, ResolveInfo o2) {
                    return String.CASE_INSENSITIVE_ORDER.compare(
                            o1.loadLabel(pm).toString(),
                            o2.loadLabel(pm).toString()
                    );
                }
            });
            Log.i(TAG, "Found " + activities.size() + " activities.");

            mRecyclerView.setAdapter(new ActivityAdapter(activities));
        }
    }

    private class ActivityHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ResolveInfo mResolveInfo;
        private TextView mNameTextView;

        public ActivityHolder(View itemView) {
            super(itemView);
            mNameTextView = (TextView) itemView;
            mNameTextView.setOnClickListener(this);
        }

        public void bindActivity(ResolveInfo resolveInfo) {
            mResolveInfo = resolveInfo;
            FragmentActivity activity = getActivity();
            if (null != activity) {
                PackageManager pm = activity.getPackageManager();
                String appName = mResolveInfo.loadLabel(pm).toString();
                mNameTextView.setText(appName);
            }
        }

        @Override
        public void onClick(View v) {
            ActivityInfo activityInfo = mResolveInfo.activityInfo;

            Intent intent=new Intent(Intent.ACTION_MAIN)
                    .setClassName(activityInfo.applicationInfo.packageName,
                            activityInfo.name);

            startActivity(intent);
        }
    }

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityHolder> {

        private final List<ResolveInfo> mActivities;

        public ActivityAdapter(List<ResolveInfo> activities) {
            mActivities = activities;
        }

        @Override
        public ActivityHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ActivityHolder(view);
        }

        @Override
        public void onBindViewHolder(ActivityHolder holder, int position) {
            ResolveInfo resolveInfo = mActivities.get(position);
            holder.bindActivity(resolveInfo);
        }

        @Override
        public int getItemCount() {
            return mActivities.size();
        }
    }
}
