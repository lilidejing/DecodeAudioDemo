package com.csw.decodeaudiodemo;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by senshan_wang on 2016/3/31.
 */
public class AudioCodec {

	private static final String TAG = "AudioCodec";
	private String encodeType;
	private String srcPath;
	private String dstPath;
	private MediaCodec mediaDecode;
	private MediaCodec mediaEncode;
	private MediaExtractor mediaExtractor;
	private ByteBuffer[] decodeInputBuffers;
	private ByteBuffer[] decodeOutputBuffers;
	private ByteBuffer[] encodeInputBuffers;
	private ByteBuffer[] encodeOutputBuffers;
	private MediaCodec.BufferInfo decodeBufferInfo;
	private MediaCodec.BufferInfo encodeBufferInfo;
	private FileOutputStream fos;
	private BufferedOutputStream bos;
	private FileInputStream fis;
	private BufferedInputStream bis;
	private ArrayList<byte[]> chunkYUVDataContainer;// YUV数据块容器
	private OnCompleteListener onCompleteListener;
	private OnProgressListener onProgressListener;
	private long fileTotalSize;
	private long decodeSize;

	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
															// Coding
	private static final int FRAME_RATE = 30; // 30 fps
	private static final int IFRAME_INTERVAL = 1; // 10 seconds between I-frames
	private static final int TIMEOUT_US = 10000;

	private int mWidth = 640;
	private int mHeight = 340;
	private int mBitRate = 444000;

	private OutputStream mOutputStream;

	public static AudioCodec newInstance() {
		return new AudioCodec();
	}

	/**
	 * 设置编码器类型
	 * 
	 * @param encodeType
	 */
	public void setEncodeType(String encodeType) {
		this.encodeType = encodeType;
	}

	/**
	 * 设置输入输出文件位置
	 * 
	 * @param srcPath
	 * @param dstPath
	 */
	public void setIOPath(String srcPath, String dstPath) {
		this.srcPath = srcPath;
		this.dstPath = dstPath;
	}

	/**
	 * 此类已经过封装 调用prepare方法 会初始化Decode 、Encode 、输入输出流 等一些列操作
	 */
	public void prepare() {

		try {

			File mFile = new File("/sdcard/test.yuv");
			if (!mFile.exists()) {
				try {
					mFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			mOutputStream = new FileOutputStream(mFile);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (encodeType == null) {
			throw new IllegalArgumentException("encodeType can't be null");
		}

		if (srcPath == null) {
			throw new IllegalArgumentException("srcPath can't be null");
		}

		if (dstPath == null) {
			throw new IllegalArgumentException("dstPath can't be null");
		}

		try {
			fos = new FileOutputStream(new File(dstPath));
			bos = new BufferedOutputStream(fos, 200 * 1024);
			File file = new File(srcPath);
			fileTotalSize = file.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
		chunkYUVDataContainer = new ArrayList<>();
		initMediaDecode();// 解码器

		if (encodeType.equals("video/avc")) {
			initH264MediaEncode();// H264编码器
		}

	}

	/**
	 * 初始化解码器
	 */
	private void initMediaDecode() {
		try {
			mediaExtractor = new MediaExtractor();// 此类可分离视频文件的音轨和视频轨道
			mediaExtractor.setDataSource(srcPath);// 媒体文件的位置
			for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {// 遍历媒体轨道
																		// 此处我们传入的是视频文件，所以也就只有一条轨道
				MediaFormat format = mediaExtractor.getTrackFormat(i);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("video")) {// 获取视频轨道
					// format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 *
					// 1024);
					mediaExtractor.selectTrack(i);// 选择此视频轨道
					mediaDecode = MediaCodec.createDecoderByType(mime);// 创建Decode解码器
					mediaDecode.configure(format, null, null, 0);
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (mediaDecode == null) {
			Log.e(TAG, "create mediaDecode failed");
			return;
		}
		mediaDecode.start();// 启动MediaCodec ，等待传入数据
		decodeInputBuffers = mediaDecode.getInputBuffers();// MediaCodec在此ByteBuffer[]中获取输入数据
		decodeOutputBuffers = mediaDecode.getOutputBuffers();// MediaCodec将解码后的数据放到此ByteBuffer[]中
																// 我们可以直接在这里面得到YUV数据
		decodeBufferInfo = new MediaCodec.BufferInfo();// 用于描述解码得到的byte[]数据的相关信息
		showLog("buffers:" + decodeInputBuffers.length);
	}

	/**
	 * 初始化H264编码器
	 */
	private void initH264MediaEncode() {
		try {

			// MediaFormat encodeFormat = MediaFormat.createAudioFormat(
			// encodeType, 44100, 2);// 参数对应-> mime type、采样率、声道数
			// encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);// 比特率
			// encodeFormat.setInteger(MediaFormat.KEY_H264_PROFILE,
			// MediaCodecInfo.CodecProfileLevel.H264ObjectLC);
			// encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 *
			// 1024);
			// mediaEncode = MediaCodec.createEncoderByType(encodeType);
			// mediaEncode.configure(encodeFormat, null, null,
			// MediaCodec.CONFIGURE_FLAG_ENCODE);

			MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE,
					mWidth, mHeight);
//			format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//					MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);// COLOR_FormatYUV420SemiPlanar
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
					MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
			format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
			format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
			format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

			Log.d(TAG, "created video format: " + format);
			mediaEncode = MediaCodec.createEncoderByType(MIME_TYPE);
			mediaEncode.configure(format, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE);
			// mSurface = mEncoder.createInputSurface();
			// Log.d(TAG, "created input surface: " + mSurface);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mediaEncode == null) {
			Log.e(TAG, "create mediaEncode failed");
			return;
		}
		// mediaEncode.start();
		mediaEncode.start();
		encodeInputBuffers = mediaEncode.getInputBuffers();
		encodeOutputBuffers = mediaEncode.getOutputBuffers();
		encodeBufferInfo = new MediaCodec.BufferInfo();
	}

	/**
	 * 初始化MPEG编码器
	 */
	private void initMPEGMediaEncode() {

	}

	private boolean codeOver = false;

	/**
	 * 开始转码 视频数据{@link #srcPath}先解码成YUV YUV数据在编码成想要得到的{@link #encodeType}视频格式
	 * mp3->YUV->H264
	 */
	public void startAsync() {
		showLog("start");

		new Thread(new DecodeRunnable()).start();
		new Thread(new EncodeRunnable()).start();

	}

	/**
	 * 将YUV数据存入{@link #chunkYUVDataContainer}
	 * 
	 * @param YUVChunk
	 *            YUV数据块
	 */
	private void putYUVData(byte[] YUVChunk) {
		synchronized (AudioCodec.class) {// 记得加锁
			chunkYUVDataContainer.add(YUVChunk);
		}
	}

	/**
	 * 在Container中{@link #chunkYUVDataContainer}取出YUV数据
	 * 
	 * @return YUV数据块
	 */
	private byte[] getYUVData() {
		synchronized (AudioCodec.class) {// 记得加锁
			showLog("getYUV:   chunkYUVDataContainer.size=="
					+ chunkYUVDataContainer.size());
			if (chunkYUVDataContainer.isEmpty()) {
				return null;
			}

			byte[] YUVChunk = chunkYUVDataContainer.get(0);// 每次取出index 0 的数据
			chunkYUVDataContainer.remove(YUVChunk);// 取出后将此数据remove掉
													// 既能保证YUV数据块的取出顺序 又能及时释放内存
			return YUVChunk;
		}
	}

	/**
	 * 解码{@link #srcPath}视频文件 得到YUV数据块
	 * 
	 * @return 是否解码完所有数据
	 */
	private void srcAudioFormatToYUV() {
		for (int i = 0; i < decodeInputBuffers.length - 1; i++) {
			int inputIndex = mediaDecode.dequeueInputBuffer(-1);// 获取可用的inputBuffer
																// -1代表一直等待，0表示不等待
																// 建议-1,避免丢帧
			if (inputIndex < 0) {
				codeOver = true;
				return;
			}

			ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];// 拿到inputBuffer
			inputBuffer.clear();// 清空之前传入inputBuffer内的数据
			int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);// MediaExtractor读取数据到inputBuffer中
			if (sampleSize < 0) {// 小于0 代表所有数据已读取完成
				codeOver = true;
			} else {
				mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);// 通知MediaDecode解码刚刚传入的数据
				mediaExtractor.advance();// MediaExtractor移动到下一取样处
				decodeSize += sampleSize;
			}
		}

		// 获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间
		// 同上-1代表一直等待，0代表不等待。此处单位为微秒
		// 此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
		int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo,
				10000);

		// showLog("decodeOutIndex:" + outputIndex);
		ByteBuffer outputBuffer;
		byte[] chunkYUV;
		while (outputIndex >= 0) {// 每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
			outputBuffer = decodeOutputBuffers[outputIndex];// 拿到用于存放YUV数据的Buffer
			chunkYUV = new byte[decodeBufferInfo.size];// BufferInfo内定义了此数据块的大小
			outputBuffer.get(chunkYUV);// 将Buffer内的数据取出到字节数组中
			outputBuffer.clear();// 数据取出后一定记得清空此Buffer
									// MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
			putYUVData(chunkYUV);// 自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码

			/*try {
				mOutputStream.write(chunkYUV);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

			mediaDecode.releaseOutputBuffer(outputIndex, false);// 此操作一定要做，不然MediaCodec用完所有的Buffer后
																// 将不能向外输出数据
			outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo,
					10000);// 再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
		}

	}

	
	
	 /**
	 * 编码YUV数据 得到{@link #encodeType}格式的视频文件，并保存到{@link #dstPath}
	 */
	private void dstAudioFormatFromYUV() {

		int inputIndex;
		ByteBuffer inputBuffer;
		int outputIndex;
		ByteBuffer outputBuffer;
		byte[] chunkVedio;
		int outBitSize;
		byte[] chunkYUV;
		byte [] tempChunkYUV=null;
		
		showLog("doEncode");
		// for (int i = 0; i < encodeInputBuffers.length - 1; i++) {

		// Log.d(TAG, "i=="+i+"   "+(encodeInputBuffers.length-1));
		chunkYUV = getYUVData();// 获取解码器所在线程输出的数据 代码后边会贴上
		
		
		if(tempChunkYUV==null){
			if (chunkYUV == null) {
//				break;
				Log.d(TAG, "chunkYUV == null");
			} else {
				Log.d(TAG, "chunkYUV != null");
				

				
				
				inputIndex = mediaEncode.dequeueInputBuffer(-1);// 同解码器
				inputBuffer = encodeInputBuffers[inputIndex];// 同解码器
		
				
				int chunkYUVLength = chunkYUV.length ;//取出来的YUV字节数据长度
				
				int tempByteLength=inputBuffer.capacity();
				
				byte tempByte[] = new byte[tempByteLength];
		
				System.arraycopy(chunkYUV, 0, tempByte, 0, tempByteLength);
				
				if(chunkYUVLength>tempByteLength){
					int tempLength=chunkYUVLength-tempByteLength;
					
					Log.d(TAG, "chunkYUVLength>tempByteLength，tempLength=="+tempLength);
					
					tempChunkYUV=new byte[tempLength];
					
					tempChunkYUV=Arrays.copyOfRange(
							chunkYUV, tempByteLength,
							chunkYUV.length);// 删掉用掉的字节数据，剩下的缓存字节数据

				}
				
				
				inputBuffer.clear();// 同解码器

				Log.d(TAG, "tempByte.length==" + tempByte.length
						+ "  inputBuffer.capacity==" + inputBuffer.capacity());
				inputBuffer.limit(tempByte.length);
				inputBuffer.put(tempByte);// YUV数据填充给inputBuffer
				mediaEncode.queueInputBuffer(inputIndex, 0, tempByte.length, 0, 0);// 通知编码器
			} // 编码
		}else{
			inputIndex = mediaEncode.dequeueInputBuffer(-1);// 同解码器
			inputBuffer = encodeInputBuffers[inputIndex];// 同解码器

			inputBuffer.clear();// 同解码器

			Log.d(TAG, "tempChunkYUV.length==" + tempChunkYUV.length
					+ "  inputBuffer.capacity==" + inputBuffer.capacity());
			inputBuffer.limit(tempChunkYUV.length);
			inputBuffer.put(tempChunkYUV);// YUV数据填充给inputBuffer
			mediaEncode.queueInputBuffer(inputIndex, 0, tempChunkYUV.length, 0, 0);// 通知编码器
		}
		
		
		// }

		outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);// 同解码器
		Log.d(TAG, "outputIndex=" + outputIndex);
		while (outputIndex >= 0) {// 同解码器

			outBitSize = encodeBufferInfo.size;

			Log.d(TAG, "outBitSize=" + outBitSize);
			// outPacketSize = outBitSize + 7;// 7为ADTS头部的大小

			outputBuffer = encodeOutputBuffers[outputIndex];// 拿到输出Buffer
			outputBuffer.position(encodeBufferInfo.offset);
			outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
			chunkVedio = new byte[outBitSize];

			outputBuffer.get(chunkVedio, 0, outBitSize);// 将编码得到的H264数据
														
			outputBuffer.position(encodeBufferInfo.offset);
			// showLog("outPacketSize:" + outPacketSize +
			// " encodeOutBufferRemain:" + outputBuffer.remaining());
			try {
				bos.write(chunkVedio, 0, chunkVedio.length);// BufferOutputStream
				Log.d(TAG, "bos.write"); // 将文件保存到内存卡中 *.H264
			} catch (IOException e) {
				e.printStackTrace();
			}

			mediaEncode.releaseOutputBuffer(outputIndex, false);
			outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo,
					10000);

		}
	}

	

	/**
	 * 释放资源
	 */
	public void release() {
		try {
			if (bos != null) {
				bos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					bos = null;
				}
			}
		}

		try {
			if (fos != null) {
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			fos = null;
		}

		if (mediaEncode != null) {
			mediaEncode.stop();
			mediaEncode.release();
			mediaEncode = null;
		}

		if (mediaDecode != null) {
			mediaDecode.stop();
			mediaDecode.release();
			mediaDecode = null;
		}

		if (mediaExtractor != null) {
			mediaExtractor.release();
			mediaExtractor = null;
		}

		if (onCompleteListener != null) {
			onCompleteListener = null;
		}

		if (onProgressListener != null) {
			onProgressListener = null;
		}
		showLog("release");
	}

	/**
	 * 解码线程
	 */
	private class DecodeRunnable implements Runnable {

		@Override
		public void run() {
			while (!codeOver) {
				srcAudioFormatToYUV();
			}
			Log.d(TAG, "解码completed");
		}
	}

	/**
	 * 编码线程
	 */
	private class EncodeRunnable implements Runnable {

		@Override
		public void run() {
			long t = System.currentTimeMillis();
			while (!codeOver || !chunkYUVDataContainer.isEmpty()) {
				dstAudioFormatFromYUV();
			}
			if (onCompleteListener != null) {
				onCompleteListener.completed();
			}
			showLog("size:" + fileTotalSize + " decodeSize:" + decodeSize
					+ "time:" + (System.currentTimeMillis() - t));
		}
	}

	/**
	 * 转码完成回调接口
	 */
	public interface OnCompleteListener {
		void completed();
	}

	/**
	 * 转码进度监听器
	 */
	public interface OnProgressListener {
		void progress();
	}

	/**
	 * 设置转码完成监听器
	 * 
	 * @param onCompleteListener
	 */
	public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
		this.onCompleteListener = onCompleteListener;
	}

	public void setOnProgressListener(OnProgressListener onProgressListener) {
		this.onProgressListener = onProgressListener;
	}

	private void showLog(String msg) {
		Log.e("AudioCodec", msg);
	}
}
