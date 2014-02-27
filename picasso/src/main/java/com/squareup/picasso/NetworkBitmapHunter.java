/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso;

//import static com.squareup.picasso.BitmapHunter.TAG;
import static com.squareup.picasso.Picasso.LoadedFrom.DISK;
import static com.squareup.picasso.Picasso.LoadedFrom.NETWORK;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import android.util.Log;

//import com.example.picasso.DiskFetcher;
import com.squareup.picasso.Downloader.Response;

class NetworkBitmapHunter extends BitmapHunter {
    static final int DEFAULT_RETRY_COUNT = 2;
    private static final int MARKER = 65536;

    private final Downloader downloader;

    private final DiskFetcher fetcher;

    int retryCount;

    public NetworkBitmapHunter(Picasso picasso, Dispatcher dispatcher, Cache cache, Stats stats,
                               Action action, Downloader downloader) {
        super(picasso, dispatcher, cache, stats, action);
        this.downloader = downloader;
        this.retryCount = DEFAULT_RETRY_COUNT;
        this.fetcher = picasso.diskFetcher;
    }

    /**
     * notify that original bitmap loaded.
     * @param bmp
     */
    private Bitmap notifyResult(Bitmap bmp) {
        Bitmap retVal = null;
        Action single = getAction();
        //List<Action> joined = getActions();
        //boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null /*&& !hasMultiple*/;

        if (!shouldDeliver) {
            return retVal;
        }
        boolean saveImage = false;

        if (single != null) {
            retVal = notifyResult(bmp, single);
        }
//		if (hasMultiple) {
//			for (int i = 0, n = joined.size(); i < n; i++) {
//				Action join = joined.get(i);
//				saveImage = deliverAction(bmp, join);
//			}
//		}
        return retVal;
    }

    private Bitmap notifyResult(Bitmap result,
                                Action action) {
        if (action == null || action.isCancelled()) {
            return null;
        }
        return action.onOriginalBitmapLoaded(result);
    }

    @Override Bitmap decode(Request data) throws IOException {

        //first load final bitmap from diskcard.
        if(fetcher != null){
            Bitmap bmp = fetcher.decode(cache,key,data);
            if (bmp != null) {
                if (Constants.DEBUG) {
                    Log.d(TAG,"decode bitmap from sdcard uri length"+data.uri.toString().length()+" key length"+key.length());
                }
                loadedFrom = DISK;
                return bmp;
            }
        }

        if(fetcher != null){
            //then load the oringal bitmap from diskcard
            Bitmap bmp = fetcher.decodeOriginalBitmap(data);
            if (bmp != null) {
                Bitmap old = bmp;
                if(Constants.DEBUG){
                    Log.i(TAG,"decode original bitmap from sdcard uri length"+data.uri.toString().length()+" key length"+key.length());
                }
                loadedFrom = DISK;
                //do the transformation if necessary
                synchronized (DECODE_LOCK) {
                    bmp = notifyResult(bmp);
                }
                //even if bmp is null,return null,because null means it fails when do transformation

                if(bmp != null && bmp != old && !old.isRecycled()){
                    old.recycle();
                }
                return bmp;
            }
        }
        boolean loadFromLocalCacheOnly = retryCount == 0;

        //download bitmap from internet
        Response response = downloader.load(data.uri, loadFromLocalCacheOnly);
        if(Constants.DEBUG){
            Log.d(TAG, "decode bitmap from internet uri length"+data.uri.toString().length()+" key length"+key.length());
        }
        if (response == null) {
            return null;
        }

        loadedFrom = response.cached ? DISK : NETWORK;

        Bitmap result = response.getBitmap();
        if (result != null) {
            return result;
        }

        InputStream is = response.getInputStream();
        if (is == null) {
            return null;
        }
        if (loadedFrom == NETWORK && response.getContentLength() > 0) {
            stats.dispatchDownloadFinished(response.getContentLength());
        }
        try {
            //at last load original bitmap from internet.
            result = decodeStream(is, data);
            if(result != null){
                Bitmap original = result;
                if(original == null){
                    return null;
                }
                if(/*saveImage && */fetcher != null){
                    //save the original bitmap
                    if(!original.isRecycled()){
                        fetcher.saveOriginalBitmap(key,data,original);
                    }else {
                        Log.e(TAG,"Oringinal bitmap is recycled,cannot be saved!");
                    }

                }
                if(Constants.DEBUG){
                    Log.d("deliverAction", "old "+original+" size:"+original.getWidth() +" "+original.getHeight());
                }
                //notify finish downloading bitmap from internet.
                synchronized (DECODE_LOCK) {
                    result = notifyResult(original);
                }
                if(Constants.DEBUG){
                    try {
                        if(result == original){
                            Log.e(TAG, "result == original");
                        }else if(result.getWidth() == original.getWidth() && result.getHeight() == original.getHeight()){
                            Log.e(TAG, "result and original are the same size");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Test error",e);
                    }
                }
                if(result != null && original != null && result != original && !original.isRecycled()){
                    original.recycle();
                }
                Log.d("deliverAction", "result "+result);
            }
            if(result == null || result.isRecycled()){
                return null;
            }

            return result;
        } finally {
            Utils.closeQuietly(is);
        }
    }

    @Override boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
        boolean hasRetries = retryCount > 0;
        if (!hasRetries) {
            return false;
        }
        retryCount--;
        return info == null || info.isConnectedOrConnecting();
    }

    private Bitmap decodeStream(InputStream stream, Request data) throws IOException {
        MarkableInputStream markStream = new MarkableInputStream(stream);
        stream = markStream;

        long mark = markStream.savePosition(MARKER);

        final BitmapFactory.Options options = createBitmapOptions(data);
        final boolean calculateSize = requiresInSampleSize(options);

        boolean isWebPFile = Utils.isWebPFile(stream);
        markStream.reset(mark);
        // When decode WebP network stream, BitmapFactory throw JNI Exception and make app crash.
        // Decode byte array instead
        if (isWebPFile) {
            byte[] bytes = Utils.toByteArray(stream);
            if (calculateSize) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                calculateInSampleSize(data.targetWidth, data.targetHeight, options);
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } else {
            if (calculateSize) {
                BitmapFactory.decodeStream(stream, null, options);
                calculateInSampleSize(data.targetWidth, data.targetHeight, options);

                markStream.reset(mark);
            }
            return BitmapFactory.decodeStream(stream, null, options);
        }
    }
}
