package com.csw.util;

import android.media.MediaCodecInfo;
import android.media.MediaMetadataRetriever;
import android.util.Log;

public class CodecUtil {

	private static final String TAG="CodecUtil";
	
	/**
	 * 判断系统是否支持当前格式yuv的color
	 * @param colorFormat
	 * @param caps
	 * @return
	 */
	 public static boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
	    	Log.i(TAG, "color==?");
	        for (int c : caps.colorFormats) {
	        	Log.i(TAG, "color=="+c);
	            if (c == colorFormat) {
	                return true;
	            }
	        }
	        return false;
	    }
	
	/* public static boolean checkColorFormat(MediaCodecInfo paramMediaCodecInfo, String paramString, int paramInt)
	  {
		 
		 
		 
	    int i = 0;
	    MediaCodecInfo.CodecCapabilities localCodecCapabilities = paramMediaCodecInfo.getCapabilitiesForType(paramString);
	    int j = 0;
	    if (j < localCodecCapabilities.colorFormats.length)
	      if (paramInt == localCodecCapabilities.colorFormats[j])
	        i = 1;
	    while (true)
	    {
	      return i;
	      j++;
	      break;
	      L.e("color format " + paramInt + " for codec " + paramMediaCodecInfo.getName() + " / " + paramString + " is not supported.");
	    }
	  }
	*/
	
	
}
