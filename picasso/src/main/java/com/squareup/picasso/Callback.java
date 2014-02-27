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

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface Callback {
  void onSuccess();

  void onError();

    /**
     *
     * New method <BR/><BR/>
     *
     * Called when (original) bitmap loaded from internet or diskcard,you can
     * do some transformation at the moment.If so you don't have to call
     * {@link RequestCreator#transform} and so on.
     * @param target
     * @param bmp
     * @return
     */
  Bitmap onOriginalBitmapLoaded(ImageView target, Bitmap bmp);

  //just for test
  boolean isTest();

  //just for test
  String getTestKey();

  public static class EmptyCallback implements Callback {

    @Override public void onSuccess() {
    }

    @Override public void onError() {
    }

	@Override
	public Bitmap onOriginalBitmapLoaded(ImageView target,Bitmap bmp) {
		return bmp;
	}

      @Override
      public boolean isTest() {
          return false;
      }

      @Override
      public String getTestKey() {
          return null;
      }
  }
}
