package com.nerdanonymous.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 使用AsyncTaskLoader代替AsyncTask，是为了保证设备在配置改变时（旋转）反复请求的问题！
 * 当然，使用AsyncTask+Fragment可使用setRetainInstance(true)解决。
 */
public class PhotoGalleryFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<GalleryItem>> {

    private static final String TAG = PhotoGalleryFragment.class.getSimpleName();
    private static final String ARG_PAGE_NUMBER = "page_number";
    private static final String ARG_QUERY = "query";
    private static final int DEFAULT_SPAN_COUNT = 3;
    private static final int DEFAULT_X = 1440;

    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(10);
    private LoaderManager mLoaderManager;
    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter mPhotoAdapter;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private ProgressBar mProgressBar;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mProgressBar = view.findViewById(R.id.photo_gallery_progress_bar);
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
                    Loader<List<GalleryItem>> loader = mLoaderManager.getLoader(0);
                    FetchItemsLoader fetchItemsLoader = (FetchItemsLoader) loader;
                    if (TextUtils.isEmpty(fetchItemsLoader.mQuery) && !recyclerView.canScrollVertically(1) && !loading) {
                        int currentPageNumber = Math.round(mPhotoAdapter.getItemCount() / 100f);
                        int nextPageNumber = currentPageNumber + 1;
                        Log.i(TAG, "Loading page: " + nextPageNumber);
                        mLoaderManager.restartLoader(0, getArgs(nextPageNumber, null), PhotoGalleryFragment.this);
                    }

                    PhotoAdapter adapter = (PhotoAdapter) recyclerView.getAdapter();
                    List<GalleryItem> galleryItems = adapter.getGalleryItems();

                    GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                    int firstPosition = manager.findFirstVisibleItemPosition();
                    for (int i = firstPosition; i != -1 && firstPosition - 10 > 0 && firstPosition - i < 10; i--) {
                        String url = galleryItems.get(i).getUrl();
                        mThumbnailDownloader.preloadImage(galleryItems.get(i).getUrl());
                        Log.i(TAG, "preload image URL: " + url);
                    }

                    int lastPosition = manager.findLastVisibleItemPosition();
                    for (int i = lastPosition; i != -1 && i - lastPosition < 10 && i < galleryItems.size(); i++) {
                        String url = galleryItems.get(i).getUrl();
                        mThumbnailDownloader.preloadImage(galleryItems.get(i).getUrl());
                        Log.i(TAG, "preload image URL: " + url);
                    }
                }
            }
        });

        setupAdapter(null);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLoaderManager = getLoaderManager();
        mLoaderManager.initLoader(0, null, this);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                BitmapDrawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit" + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                Log.d(TAG, "onQueryTextChange" + newText);
                mScheduledThreadPoolExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        CharSequence query = searchView.getQuery();
                        if (TextUtils.equals(query, newText)) {// To prevent invalid request
                            QueryPreferences.setStoredQuery(getActivity(), newText);
                            updateItems();
                        }
                    }
                }, 500, TimeUnit.MILLISECONDS);
                return true;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQueryHint(TextUtils.isEmpty(query) ? null : query);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        mLoaderManager.restartLoader(0, getArgs(0, query), PhotoGalleryFragment.this);
    }

    private Bundle getArgs(int pageNumber, String query) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_PAGE_NUMBER, pageNumber);
        bundle.putString(ARG_QUERY, query);
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
                String query = args.getString(ARG_QUERY);
                fetchItemsLoader = new FetchItemsLoader(context, pageNumber);
                if (null != query) {
                    fetchItemsLoader = new FetchItemsLoader(context, query);
                }
            } else {
                fetchItemsLoader = new FetchItemsLoader(context);
            }
        }

        if (null != getActivity()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            });
        }

        return fetchItemsLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<GalleryItem>> loader, List<GalleryItem> galleryItems) {
        Log.i(TAG, "onLoadFinished");
        mProgressBar.setVisibility(View.GONE);

        if (null != mPhotoAdapter) {
            FetchItemsLoader fetchItemsLoader = (FetchItemsLoader) loader;
            if (1 == fetchItemsLoader.mPageNumber) {
                mPhotoAdapter.getGalleryItems().clear();
            }
            setupAdapter(galleryItems);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<GalleryItem>> loader) {
        Log.i(TAG, "onLoaderReset");
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = itemView.findViewById(R.id.photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems = new ArrayList<>();

        public List<GalleryItem> getGalleryItems() {
            return mGalleryItems;
        }

        public void addItems(List<GalleryItem> galleryItems) {
            mGalleryItems.addAll(galleryItems);
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_gallery, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.mipmap.ic_launcher);
            holder.bindDrawable(placeHolder);
            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private static class FetchItemsLoader extends AsyncTaskLoader<List<GalleryItem>> {

        private int mPageNumber = 1;
        private String mQuery;

        public FetchItemsLoader(@NonNull Context context) {
            this(context, 1);
        }

        public FetchItemsLoader(@NonNull Context context, int pageNumber) {
            super(context);
            if (pageNumber > 0) {
                mPageNumber = pageNumber;
            }
        }

        public FetchItemsLoader(@NonNull Context context, String query) {
            super(context);
            mQuery = query;
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
            if (TextUtils.isEmpty(mQuery)) {
                return new FlickrFetcher().fetchRecentPhotos(mPageNumber);
            } else {
                return new FlickrFetcher().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onStopLoading() {
            Log.i(TAG, "onStopLoading");
            cancelLoad();
        }
    }
}
