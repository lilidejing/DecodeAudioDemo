package com.csw.decodeaudiodemo;

import android.app.Activity;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		String path=Environment.getExternalStorageDirectory().getAbsolutePath();
		final AudioCodec audioCodec=AudioCodec.newInstance();
		audioCodec.setEncodeType(MIME_TYPE);
		audioCodec.setIOPath(path + "/input2.mp4", path + "/encode_test.h264");
		audioCodec.prepare();
		audioCodec.startAsync();
		audioCodec.setOnCompleteListener(new AudioCodec.OnCompleteListener() {
		    @Override
		    public void completed() {
		        audioCodec.release();
		    }
		});
		
	}

	
}
