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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;

class ResourceBitmapHunter extends BitmapHunter {
  private final Context context;

  ResourceBitmapHunter(Context context, Picasso picasso, Dispatcher dispatcher, Cache cache,
      Stats stats, Action action) {
    super(picasso, dispatcher, cache, stats, action);
    this.context = context;
  }

    /**
     * notify that original bitmap loaded.
     * @param bmp
     */
    private Bitmap notifyResult(Bitmap bmp) {
        Bitmap retVal = null;
        Action single = getAction();
        List<Action> joined = getActions();

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
      Resources res = Utils.getResources(context, data);
      int id = Utils.getResourceId(res, data);
      Bitmap bmp = decodeResource(res, id, data);
      if (bmp != null) {
          Bitmap old = bmp;
          //Log.i(TAG, "decode original bitmap from sdcard uri length" + data.uri.toString().length() + " key length" + key.length());
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
      return bmp;
  }

//    @Override Bitmap decode(Request data) throws IOException {
//    Resources res = Utils.getResources(context, data);
//    int id = Utils.getResourceId(res, data);
//    return decodeResource(res, id, data);
//  }

  @Override Picasso.LoadedFrom getLoadedFrom() {
    return DISK;
  }

  private Bitmap decodeResource(Resources resources, int id, Request data) {
    final BitmapFactory.Options options = createBitmapOptions(data);
    if (requiresInSampleSize(options)) {
      BitmapFactory.decodeResource(resources, id, options);
      calculateInSampleSize(data.targetWidth, data.targetHeight, options);
    }
    return BitmapFactory.decodeResource(resources, id, options);
  }
}
