package com.nethergrim.vk.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.nethergrim.vk.models.User;
import com.nethergrim.vk.utils.UserUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author andreydrobyazko on 4/7/15.
 */
public class PicassoImageLoaderImpl implements ImageLoader {

    private Context context;
    private LruCache<String, Bitmap> mBitmapLruCache;


    public PicassoImageLoaderImpl(Context context) {
        this.context = context;
    }

    @Override
    public Observable<Bitmap> getUserAvatar(@NonNull User user) {
        final String url = UserUtils.getStablePhotoUrl(user);
        return getBitmapObservable(url);
    }

    @Override
    public Observable<Bitmap> getBitmap(@NonNull String url) {
        return getBitmapObservable(url);
    }

    @Override
    public Observable<Bitmap> getBitmap(@NonNull User user) {
        return getBitmap(UserUtils.getStablePhotoUrl(user));
    }

    @Override
    public void cacheToMemory(String url) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Picasso.with(context).load(url).config(Bitmap.Config.RGB_565).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    addToCache(url, bitmap);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        });

    }

    @Override
    @Nullable
    public Bitmap getBitmapSync(String url) {
        if (mBitmapLruCache != null) {
            Bitmap result = mBitmapLruCache.get(url);
            if (result != null) {
                return result;
            }
        }
        try {
            return Picasso.with(context).load(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addToCache(String url, Bitmap bitmap) {
        if (mBitmapLruCache == null) {
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;
            mBitmapLruCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
        if (mBitmapLruCache.get(url) == null) {
            mBitmapLruCache.put(url, bitmap);
        }
    }

    private Observable<Bitmap> getBitmapObservable(String src) {
        Observable<Bitmap> bitmapObservable = Observable.create(
                new Observable.OnSubscribe<Bitmap>() {
                    @Override
                    public void call(final Subscriber<? super Bitmap> subscriber) {
                        if (subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                            return;
                        }

                        if (mBitmapLruCache != null) {
                            Bitmap result = mBitmapLruCache.get(src);
                            if (result != null) {
                                subscriber.onNext(result);
                                subscriber.onCompleted();
                            }
                        }

                        try {
                            URL url = new URL(src);
                            HttpURLConnection connection
                                    = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();
                            Bitmap myBitmap = BitmapFactory.decodeStream(input);

                            addToCache(src, myBitmap);
                            subscriber.onNext(myBitmap);
                            subscriber.onCompleted();
                        } catch (IOException e) {
                            subscriber.onError(e);
                        }
                    }
                });
        bitmapObservable.observeOn(Schedulers.io());
        bitmapObservable.subscribeOn(AndroidSchedulers.mainThread());
        return bitmapObservable;
    }

}
