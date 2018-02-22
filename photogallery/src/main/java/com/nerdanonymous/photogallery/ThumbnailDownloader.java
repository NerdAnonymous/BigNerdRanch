package com.nerdanonymous.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = ThumbnailDownloader.class.getSimpleName();
    private static final int MESSAGE_DOWNLOAD = 8;
    private static final int MESSAGE_PRELOAD = 9;

    private Boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private final LruCache<String, Bitmap> mLruCache;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> thumbnailDownloadListener) {
        mThumbnailDownloadListener = thumbnailDownloadListener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;

        long maxMemory = Runtime.getRuntime().maxMemory();
        long mCacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(Long.valueOf(mCacheSize).intValue()) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_DOWNLOAD:
                        T target = (T) msg.obj;
                        Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                        handleRequest(target);
                        break;
                    case MESSAGE_PRELOAD:
                        String url = (String) msg.obj;
                        Log.i(TAG, "Got a preload for URL: " + url);
                        downloadBitmap(url);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL:" + url);

        if (TextUtils.isEmpty(url)) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public void preloadImage(String url) {
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url)
                .sendToTarget();
    }

    /**
     * After thinking about this, I think we should be clearing the request map in the clearQueue method.
     * The request map is storing PhotoHolder objects, which have a reference to a view object (one row in the RecyclerView).
     * Any View object on Android has a reference to a Context (which is going to be the PhotoGalleryActivity).
     * So, when you rotate (as an example), it’s possible that old PhotoHolder objects will stay in that request map forever.
     * That means that there is a memory leak. The activity is being leaked.
     */
    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestHandler.removeMessages(MESSAGE_PRELOAD);
        mRequestMap.clear();
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);

        if (TextUtils.isEmpty(url)) {
            return;
        }

        final Bitmap bitmap = downloadBitmap(url);
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.equals(mRequestMap.get(target), url) || mHasQuit) {
                    return;
                }

                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
            }
        });

    }

    private Bitmap downloadBitmap(String url) {
        Bitmap bitmap;
        synchronized (mLruCache) {
            bitmap = mLruCache.get(url);
        }
        if (null != bitmap) {
            return bitmap;
        }

        try {
            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            synchronized (mLruCache) {
                mLruCache.put(url, bitmap);
            }
            Log.i(TAG, "Bitmap created & cached image: " + url);
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }

        return bitmap;
    }
}
