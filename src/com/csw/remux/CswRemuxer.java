package com.csw.remux;

import android.util.Log;

public class CswRemuxer {
	private static final String TAG = "DEMUX";

	static {

		try {
			Log.d(TAG, "Ready to load libremux.so...");
			System.loadLibrary("remux");
			Log.d(TAG, "load libremux.so OK...");
		} catch (UnsatisfiedLinkError ule) {
			System.err.println("WARNING: Could not load library!");
		}
	}

	public static native int DoReMux(String strInMp4, String strInAAC,
			String strOutMp4);

	public static native int DoFormatConvert(String strInFile, String strOutFile);
}