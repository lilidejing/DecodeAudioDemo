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
	private ArrayList<byte[]> chunkYUVDataContainer;// YUV���ݿ�����
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
	 * ���ñ���������
	 * 
	 * @param encodeType
	 */
	public void setEncodeType(String encodeType) {
		this.encodeType = encodeType;
	}

	/**
	 * ������������ļ�λ��
	 * 
	 * @param srcPath
	 * @param dstPath
	 */
	public void setIOPath(String srcPath, String dstPath) {
		this.srcPath = srcPath;
		this.dstPath = dstPath;
	}

	/**
	 * �����Ѿ�����װ ����prepare���� ���ʼ��Decode ��Encode ����������� ��һЩ�в���
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
		initMediaDecode();// ������

		if (encodeType.equals("video/avc")) {
			initH264MediaEncode();// H264������
		}

	}

	/**
	 * ��ʼ��������
	 */
	private void initMediaDecode() {
		try {
			mediaExtractor = new MediaExtractor();// ����ɷ�����Ƶ�ļ����������Ƶ���
			mediaExtractor.setDataSource(srcPath);// ý���ļ���λ��
			for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {// ����ý����
																		// �˴����Ǵ��������Ƶ�ļ�������Ҳ��ֻ��һ�����
				MediaFormat format = mediaExtractor.getTrackFormat(i);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("video")) {// ��ȡ��Ƶ���
					// format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 *
					// 1024);
					mediaExtractor.selectTrack(i);// ѡ�����Ƶ���
					mediaDecode = MediaCodec.createDecoderByType(mime);// ����Decode������
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
		mediaDecode.start();// ����MediaCodec ���ȴ���������
		decodeInputBuffers = mediaDecode.getInputBuffers();// MediaCodec�ڴ�ByteBuffer[]�л�ȡ��������
		decodeOutputBuffers = mediaDecode.getOutputBuffers();// MediaCodec�����������ݷŵ���ByteBuffer[]��
																// ���ǿ���ֱ����������õ�YUV����
		decodeBufferInfo = new MediaCodec.BufferInfo();// ������������õ���byte[]���ݵ������Ϣ
		showLog("buffers:" + decodeInputBuffers.length);
	}

	/**
	 * ��ʼ��H264������
	 */
	private void initH264MediaEncode() {
		try {

			// MediaFormat encodeFormat = MediaFormat.createAudioFormat(
			// encodeType, 44100, 2);// ������Ӧ-> mime type�������ʡ�������
			// encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);// ������
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
	 * ��ʼ��MPEG������
	 */
	private void initMPEGMediaEncode() {

	}

	private boolean codeOver = false;

	/**
	 * ��ʼת�� ��Ƶ����{@link #srcPath}�Ƚ����YUV YUV�����ڱ������Ҫ�õ���{@link #encodeType}��Ƶ��ʽ
	 * mp3->YUV->H264
	 */
	public void startAsync() {
		showLog("start");

		new Thread(new DecodeRunnable()).start();
		new Thread(new EncodeRunnable()).start();

	}

	/**
	 * ��YUV���ݴ���{@link #chunkYUVDataContainer}
	 * 
	 * @param YUVChunk
	 *            YUV���ݿ�
	 */
	private void putYUVData(byte[] YUVChunk) {
		synchronized (AudioCodec.class) {// �ǵü���
			chunkYUVDataContainer.add(YUVChunk);
		}
	}

	/**
	 * ��Container��{@link #chunkYUVDataContainer}ȡ��YUV����
	 * 
	 * @return YUV���ݿ�
	 */
	private byte[] getYUVData() {
		synchronized (AudioCodec.class) {// �ǵü���
			showLog("getYUV:   chunkYUVDataContainer.size=="
					+ chunkYUVDataContainer.size());
			if (chunkYUVDataContainer.isEmpty()) {
				return null;
			}

			byte[] YUVChunk = chunkYUVDataContainer.get(0);// ÿ��ȡ��index 0 ������
			chunkYUVDataContainer.remove(YUVChunk);// ȡ���󽫴�����remove��
													// ���ܱ�֤YUV���ݿ��ȡ��˳�� ���ܼ�ʱ�ͷ��ڴ�
			return YUVChunk;
		}
	}

	/**
	 * ����{@link #srcPath}��Ƶ�ļ� �õ�YUV���ݿ�
	 * 
	 * @return �Ƿ��������������
	 */
	private void srcAudioFormatToYUV() {
		for (int i = 0; i < decodeInputBuffers.length - 1; i++) {
			int inputIndex = mediaDecode.dequeueInputBuffer(-1);// ��ȡ���õ�inputBuffer
																// -1����һֱ�ȴ���0��ʾ���ȴ�
																// ����-1,���ⶪ֡
			if (inputIndex < 0) {
				codeOver = true;
				return;
			}

			ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];// �õ�inputBuffer
			inputBuffer.clear();// ���֮ǰ����inputBuffer�ڵ�����
			int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);// MediaExtractor��ȡ���ݵ�inputBuffer��
			if (sampleSize < 0) {// С��0 �������������Ѷ�ȡ���
				codeOver = true;
			} else {
				mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);// ֪ͨMediaDecode����ոմ��������
				mediaExtractor.advance();// MediaExtractor�ƶ�����һȡ����
				decodeSize += sampleSize;
			}
		}

		// ��ȡ����õ���byte[]���� ����BufferInfo�����ѽ��� 10000ͬ��Ϊ�ȴ�ʱ��
		// ͬ��-1����һֱ�ȴ���0�����ȴ����˴���λΪ΢��
		// �˴����鲻Ҫ��-1 ��Щʱ��û�������������ô���ͻ�һֱ������ �ȴ�
		int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo,
				10000);

		// showLog("decodeOutIndex:" + outputIndex);
		ByteBuffer outputBuffer;
		byte[] chunkYUV;
		while (outputIndex >= 0) {// ÿ�ν�����ɵ����ݲ�һ����һ���³� ������whileѭ������֤�������³���������
			outputBuffer = decodeOutputBuffers[outputIndex];// �õ����ڴ��YUV���ݵ�Buffer
			chunkYUV = new byte[decodeBufferInfo.size];// BufferInfo�ڶ����˴����ݿ�Ĵ�С
			outputBuffer.get(chunkYUV);// ��Buffer�ڵ�����ȡ�����ֽ�������
			outputBuffer.clear();// ����ȡ����һ���ǵ���մ�Buffer
									// MediaCodec��ѭ��ʹ����ЩBuffer�ģ�������´λ�õ�ͬ��������
			putYUVData(chunkYUV);// �Լ�����ķ����������������ڵ��̻߳�ȡ����,�������������

			/*try {
				mOutputStream.write(chunkYUV);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

			mediaDecode.releaseOutputBuffer(outputIndex, false);// �˲���һ��Ҫ������ȻMediaCodec�������е�Buffer��
																// �����������������
			outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo,
					10000);// �ٴλ�ȡ���ݣ����û�����������outputIndex=-1 ѭ������
		}

	}

	
	
	 /**
	 * ����YUV���� �õ�{@link #encodeType}��ʽ����Ƶ�ļ��������浽{@link #dstPath}
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
		chunkYUV = getYUVData();// ��ȡ�����������߳���������� �����߻�����
		
		
		if(tempChunkYUV==null){
			if (chunkYUV == null) {
//				break;
				Log.d(TAG, "chunkYUV == null");
			} else {
				Log.d(TAG, "chunkYUV != null");
				

				
				
				inputIndex = mediaEncode.dequeueInputBuffer(-1);// ͬ������
				inputBuffer = encodeInputBuffers[inputIndex];// ͬ������
		
				
				int chunkYUVLength = chunkYUV.length ;//ȡ������YUV�ֽ����ݳ���
				
				int tempByteLength=inputBuffer.capacity();
				
				byte tempByte[] = new byte[tempByteLength];
		
				System.arraycopy(chunkYUV, 0, tempByte, 0, tempByteLength);
				
				if(chunkYUVLength>tempByteLength){
					int tempLength=chunkYUVLength-tempByteLength;
					
					Log.d(TAG, "chunkYUVLength>tempByteLength��tempLength=="+tempLength);
					
					tempChunkYUV=new byte[tempLength];
					
					tempChunkYUV=Arrays.copyOfRange(
							chunkYUV, tempByteLength,
							chunkYUV.length);// ɾ���õ����ֽ����ݣ�ʣ�µĻ����ֽ�����

				}
				
				
				inputBuffer.clear();// ͬ������

				Log.d(TAG, "tempByte.length==" + tempByte.length
						+ "  inputBuffer.capacity==" + inputBuffer.capacity());
				inputBuffer.limit(tempByte.length);
				inputBuffer.put(tempByte);// YUV��������inputBuffer
				mediaEncode.queueInputBuffer(inputIndex, 0, tempByte.length, 0, 0);// ֪ͨ������
			} // ����
		}else{
			inputIndex = mediaEncode.dequeueInputBuffer(-1);// ͬ������
			inputBuffer = encodeInputBuffers[inputIndex];// ͬ������

			inputBuffer.clear();// ͬ������

			Log.d(TAG, "tempChunkYUV.length==" + tempChunkYUV.length
					+ "  inputBuffer.capacity==" + inputBuffer.capacity());
			inputBuffer.limit(tempChunkYUV.length);
			inputBuffer.put(tempChunkYUV);// YUV��������inputBuffer
			mediaEncode.queueInputBuffer(inputIndex, 0, tempChunkYUV.length, 0, 0);// ֪ͨ������
		}
		
		
		// }

		outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);// ͬ������
		Log.d(TAG, "outputIndex=" + outputIndex);
		while (outputIndex >= 0) {// ͬ������

			outBitSize = encodeBufferInfo.size;

			Log.d(TAG, "outBitSize=" + outBitSize);
			// outPacketSize = outBitSize + 7;// 7ΪADTSͷ���Ĵ�С

			outputBuffer = encodeOutputBuffers[outputIndex];// �õ����Buffer
			outputBuffer.position(encodeBufferInfo.offset);
			outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
			chunkVedio = new byte[outBitSize];

			outputBuffer.get(chunkVedio, 0, outBitSize);// ������õ���H264����
														
			outputBuffer.position(encodeBufferInfo.offset);
			// showLog("outPacketSize:" + outPacketSize +
			// " encodeOutBufferRemain:" + outputBuffer.remaining());
			try {
				bos.write(chunkVedio, 0, chunkVedio.length);// BufferOutputStream
				Log.d(TAG, "bos.write"); // ���ļ����浽�ڴ濨�� *.H264
			} catch (IOException e) {
				e.printStackTrace();
			}

			mediaEncode.releaseOutputBuffer(outputIndex, false);
			outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo,
					10000);

		}
	}

	

	/**
	 * �ͷ���Դ
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
	 * �����߳�
	 */
	private class DecodeRunnable implements Runnable {

		@Override
		public void run() {
			while (!codeOver) {
				srcAudioFormatToYUV();
			}
			Log.d(TAG, "����completed");
		}
	}

	/**
	 * �����߳�
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
	 * ת����ɻص��ӿ�
	 */
	public interface OnCompleteListener {
		void completed();
	}

	/**
	 * ת����ȼ�����
	 */
	public interface OnProgressListener {
		void progress();
	}

	/**
	 * ����ת����ɼ�����
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
