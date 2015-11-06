package com.tequila.weixin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.media.MediaRecorder;

public class AudioManager {

	private MediaRecorder mMediaRecorder;
	private String mDir;
	private String mCurrentFilePath;
	
	private static AudioManager mInstance;
	private boolean isPrepared;
	
	public AudioManager(String dir) {
		
		mDir = dir;
	}
	
	/**
	 * 回调准备完毕
	 * @author Tequila
	 * 
	 * */
	
	public interface AudioStateListener {
		
		void wellPrepared();
	}
	
	public AudioStateListener mListener;
	
	public void setOnAudioStateListener(AudioStateListener listener) {
		
		mListener = listener;
	}
	
	public static AudioManager getInstance(String dir) {
		
		if (mInstance == null) {
			synchronized (AudioManager.class) {
				
				if (mInstance == null) {
					mInstance = new AudioManager(dir);
				}
			}
		}
		
		return mInstance;
	}
	
	public void prepareAudio() { // 准备
		
		try {
			
			isPrepared = false;
			
			File dir = new File(mDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			String fileName = generateFileName();
			File file = new File(dir, fileName);
			
			mCurrentFilePath = file.getAbsolutePath();
			mMediaRecorder = new MediaRecorder();
			// 设置输出文件
			mMediaRecorder.setOutputFile(file.getAbsolutePath());
			// 设置MediaRecorder的音频源为麦克风
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			// 设置音频的格式
			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			// 设置音频的编码为amr
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			
			mMediaRecorder.prepare();
			mMediaRecorder.start();
			
			// 准备结束
			isPrepared = true;
			if (mListener != null) {
				mListener.wellPrepared();
			}
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 随机生成文件名称
	 * @author Tequila
	 * */
	private String generateFileName() {
		
		return UUID.randomUUID().toString() + ".amr";
	}

	public int getVoiceLevel(int maxLevel) { // 音量等级
		
		if (isPrepared) {
			
			try {
				// mMediaRecorder.getMaxAmplitude() 1-32767
				return maxLevel * mMediaRecorder.getMaxAmplitude() / 32768 + 1;
			} catch (Exception e) {
				return 0;
			}
		}
		
		return 1;
	}
	
	public void release() { //释放
		
		mMediaRecorder.stop();
		mMediaRecorder.release();
		mMediaRecorder = null;
	}
	
	public void cancel() { 	// 取消
		
		release();
		
		if (mCurrentFilePath != null) {
			
			File file = new File(mCurrentFilePath);
			file.delete();
			mCurrentFilePath = null;
		}
	}

	public String getCurrentFilePath() {
		
		return mCurrentFilePath;
	}
}
