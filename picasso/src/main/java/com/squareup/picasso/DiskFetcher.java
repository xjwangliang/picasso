package com.squareup.picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Request;
import com.squareup.picasso.Utils;

public class DiskFetcher {
	
	//public static DiskFetcher 
	private String cacheDir;  

	public DiskFetcher(String cacheDir){
		this.cacheDir = cacheDir;
	}
	
	public Bitmap decode(Cache cache,String picassoKey,Request request){
		Bitmap bitmap = null;
//		String realKey = Utils.encodeCachedKey(picassoKey);
//		if(cache != null){
//			bitmap = cache.get(realKey);
//		}
		if(bitmap == null){
			try {
				bitmap = decodeStream(picassoKey);
			} catch (IOException e) {
				//e.printStackTrace();
				//Log.e(tag, msg)
			}
		}
		return bitmap;
	}


    /**
     * decode original bitmap(key is encoded uri)
     * @param request
     * @return
     */
	public Bitmap decodeOriginalBitmap(Request request){
		Bitmap bitmap = null;
		String oiginalBmpkey = Utils.encodeCachedKey(request.uri.toString());
		if(bitmap == null){
			try {
				bitmap = decodeStream(oiginalBmpkey);
			} catch (IOException e) {
			}
		}
		return bitmap;
	}

    /**
     * save original bitmap(key is encoded uri)
     * @param request
     * @param bitmap
     */
	public void saveOriginalBitmap(String key,Request request,Bitmap bitmap){
        //don't have do save bitmap because cache#set() will do
//        if(key.equals(request.uri.toString())){
//            //Log.d("DiskFetcher", "saveOriginalBitmap error for same length");
//           return;
//        }

		FileOutputStream os = null;
		try {
			String oiginalBmpkey = Utils.encodeCachedKey(request.uri.toString());
			File file = getFile(oiginalBmpkey);
			os = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(os != null){
				try {
					os.flush();
					os.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
				
			}
		}
       
	}
	
	
	private Bitmap decodeStream(String key) throws IOException {
		// final BitmapFactory.Options options = createBitmapOptions(data);
		// final boolean calculateSize = requiresInSampleSize(options);

		// Decode byte array instead
		// if (calculateSize) {
		// BitmapFactory.decodeStream(stream);
		// BitmapFactory.decodeStream(stream, null, options);
		// calculateInSampleSize(data.targetWidth, data.targetHeight, options);
		// }
        File file = getFile(key);
        if(!file.exists()){
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
	}

	private File getFile(String key) {
		return new File(cacheDir, key);
	}
}
