package com.csw.decodeaudiodemo;

import com.csw.util.FileUtil;

import android.app.Activity;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG="MainActivity";
	
	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private int mHeight;//视频的高
    private int mWidth;//视频的宽
    private int mBitrate;//视频的比特率
	
	/**
	 * 源视频文件
	 */
	private String srcFilePath="/sdcard/input.mpg";//源文件
	/**
	 * 目的文件
	 */
	private String dstFilePath="/sdcard/output.h264";//目的文件
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

		MediaMetadataRetriever retr = new MediaMetadataRetriever();  
	    retr.setDataSource(srcFilePath);  
	    String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); // 视频高度  
	    String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); // 视频宽度  
	    String bitrate=retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);//视频比特率
	    String durationString=retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);//获取视频持续时间 
	    
	    Log.d(width, "Get video width="+width+"   height="+height+"   bitrate="+bitrate+"   time="+durationString);
	   
	    if(height!=null&&width!=null){
	    	mHeight=Integer.parseInt(height);
	    	mWidth=Integer.parseInt(width);
	    }else{
	    	Toast.makeText(MainActivity.this, "获取不到视频的宽高信息，无法解码", 2000).show();
	    	Log.d(TAG,"获取不到视频的宽高信息，无法解码");
	    	this.finish();
	    }
		if(bitrate!=null){
			mBitrate=Integer.parseInt(bitrate);
		}else{
			
			if(durationString!=null){//视频播放时长
				long videoTime=Long.parseLong(durationString);
				double fileSize=FileUtil.getFileOrFilesSize(srcFilePath,3);
				
				mBitrate=(int) (fileSize*1024*8/(videoTime/1000)*1000);//文件大小（MB为单位）× 1024 × 8 / 影片总长度（秒为单位） = 码率（Kbps）,这里的码率是视频码率和音频码率的总和
				Log.d(TAG, "计算的视频比特率为："+mBitrate);//1361344      455902  640 344
			}else{
				
				if(mHeight<720){
					mBitrate=1024000;
				}else{
					mBitrate=3096000;
				}
				Log.d(TAG, "获取不到视频比特率，设置为："+mBitrate);
			}
		}
		
		
		if(retr!=null){
			retr.release();
		}
		final VideoCodec audioCodec=VideoCodec.newInstance();
		audioCodec.setEncodeType(MIME_TYPE);
		audioCodec.setEncodeHeightOrWidth(mWidth, mHeight,mBitrate);
		
		audioCodec.setIOPath(srcFilePath, dstFilePath);
		audioCodec.prepare();
		audioCodec.startAsync();
		audioCodec.setOnCompleteListener(new VideoCodec.OnCompleteListener() {
		    @Override
		    public void completed() {
		        audioCodec.release();
		    }
		});
		
	}

	
}
