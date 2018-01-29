package com.nerdanonymous.photogallery;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用AsyncTaskLoader代替AsyncTask，是为了保证设备在配置改变时（旋转）反复请求的问题！
 * 当然，使用AsyncTask+Fragment可使用setRetainInstance(true)解决。
 */
public class PhotoGalleryFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<GalleryItem>> {

    private static final String TAG = PhotoGalleryFragment.class.getSimpleName();
    private static final String ARG_PAGE_NUMBER = "page_number";
    private static final int DEFAULT_SPAN_COUNT = 3;
    private static final int DEFAULT_X = 1440;

    private LoaderManager mLoaderManager;
    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter mPhotoAdapter;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLoaderManager = getLoaderManager();
        mLoaderManager.initLoader(0, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = view.findViewById(R.id.photo_gallery_recycle_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), DEFAULT_SPAN_COUNT));
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                int newSpanCount = (int) Math.floor(displayMetrics.widthPixels * DEFAULT_SPAN_COUNT / DEFAULT_X);
                if (newSpanCount != DEFAULT_SPAN_COUNT) {
                    GridLayoutManager gridLayoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                    gridLayoutManager.setSpanCount(newSpanCount);
                }
            }
        });
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    boolean loading = mLoaderManager.hasRunningLoaders();

                    if (!recyclerView.canScrollVertically(1) && !loading) {
                        int currentPageNumber = mPhotoAdapter.getItemCount() / 100;
                        int nextPageNumber = currentPageNumber + 1;
                        Log.i(TAG, "Loading page: " + nextPageNumber);
                        mLoaderManager.initLoader(0, getPageNumber(nextPageNumber), PhotoGalleryFragment.this);
                    }
                }
            }
        });

        setupAdapter(null);

        return view;
    }

    private Bundle getPageNumber(int pageNumber) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_PAGE_NUMBER, pageNumber);
        return bundle;
    }

    private void setupAdapter(List<GalleryItem> galleryItems) {
        if (isAdded()) {
            if (null == mPhotoAdapter) {
                mPhotoAdapter = new PhotoAdapter();
                mPhotoRecyclerView.setAdapter(mPhotoAdapter);
            } else {
                mPhotoAdapter.addItems(galleryItems);
                mPhotoAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public Loader<List<GalleryItem>> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCreateLoader");
        FetchItemsLoader fetchItemsLoader = null;
        Context context = getContext();

        if (null != context) {
            if (null != args) {
                int pageNumber = args.getInt(ARG_PAGE_NUMBER);
                fetchItemsLoader = new FetchItemsLoader(context, pageNumber);
            } else {
                fetchItemsLoader = new FetchItemsLoader(context);
            }
        }

        return fetchItemsLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<GalleryItem>> loader, List<GalleryItem> galleryItems) {
        Log.i(TAG, "onLoadFinished");
        if (null != mPhotoAdapter) {
            setupAdapter(galleryItems);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<GalleryItem>> loader) {
        Log.i(TAG, "onLoaderReset");
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mTitleTextView.setText(galleryItem.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems = new ArrayList<>();

        public void addItems(List<GalleryItem> galleryItems) {
            mGalleryItems.addAll(galleryItems);
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private static class FetchItemsLoader extends AsyncTaskLoader<List<GalleryItem>> {

        private int mPageNumber = 1;

        public FetchItemsLoader(@NonNull Context context) {
            this(context, 1);
        }

        public FetchItemsLoader(@NonNull Context context, int pageNumber) {
            super(context);
            if (pageNumber > 0) {
                mPageNumber = pageNumber;
            }
        }

        @Override
        protected void onStartLoading() {
            Log.i(TAG, "onStartLoading");
            forceLoad();
        }

        @Nullable
        @Override
        public List<GalleryItem> loadInBackground() {
            Log.i(TAG, "loadInBackground");
            return new FlickrFetcher().fetchItems(mPageNumber);
        }

        @Override
        protected void onStopLoading() {
            Log.i(TAG, "onStopLoading");
            cancelLoad();
        }
    }
}
