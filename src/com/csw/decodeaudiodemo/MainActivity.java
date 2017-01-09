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
    private int mHeight;//��Ƶ�ĸ�
    private int mWidth;//��Ƶ�Ŀ�
    private int mBitrate;//��Ƶ�ı�����
	
	/**
	 * Դ��Ƶ�ļ�
	 */
	private String srcFilePath="/sdcard/input.mpg";//Դ�ļ�
	/**
	 * Ŀ���ļ�
	 */
	private String dstFilePath="/sdcard/output.h264";//Ŀ���ļ�
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

		MediaMetadataRetriever retr = new MediaMetadataRetriever();  
	    retr.setDataSource(srcFilePath);  
	    String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); // ��Ƶ�߶�  
	    String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); // ��Ƶ���  
	    String bitrate=retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);//��Ƶ������
	    String durationString=retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);//��ȡ��Ƶ����ʱ�� 
	    
	    Log.d(width, "Get video width="+width+"   height="+height+"   bitrate="+bitrate+"   time="+durationString);
	   
	    if(height!=null&&width!=null){
	    	mHeight=Integer.parseInt(height);
	    	mWidth=Integer.parseInt(width);
	    }else{
	    	Toast.makeText(MainActivity.this, "��ȡ������Ƶ�Ŀ����Ϣ���޷�����", 2000).show();
	    	Log.d(TAG,"��ȡ������Ƶ�Ŀ����Ϣ���޷�����");
	    	this.finish();
	    }
		if(bitrate!=null){
			mBitrate=Integer.parseInt(bitrate);
		}else{
			
			if(durationString!=null){//��Ƶ����ʱ��
				long videoTime=Long.parseLong(durationString);
				double fileSize=FileUtil.getFileOrFilesSize(srcFilePath,3);
				
				mBitrate=(int) (fileSize*1024*8/(videoTime/1000)*1000);//�ļ���С��MBΪ��λ���� 1024 �� 8 / ӰƬ�ܳ��ȣ���Ϊ��λ�� = ���ʣ�Kbps��,�������������Ƶ���ʺ���Ƶ���ʵ��ܺ�
				Log.d(TAG, "�������Ƶ������Ϊ��"+mBitrate);//1361344      455902  640 344
			}else{
				
				if(mHeight<720){
					mBitrate=1024000;
				}else{
					mBitrate=3096000;
				}
				Log.d(TAG, "��ȡ������Ƶ�����ʣ�����Ϊ��"+mBitrate);
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
