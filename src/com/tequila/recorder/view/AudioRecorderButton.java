package com.tequila.recorder.view;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.tequila.weixin.AudioManager;
import com.tequila.weixin.AudioManager.AudioStateListener;
import com.tequila.weixin.DialogManager;
import com.tequila.weixin.R;

public class AudioRecorderButton extends Button implements AudioStateListener {

	private static final int DISTANCE_Y_CANCEL = 50;
	private static final int STATE_NORMAL = 1; // Ĭ��״̬
	private static final int STATE_RECORDING = 2; // ¼��״̬
	private static final int STATE_WANT_TO_CANCEL = 3; // ȡ��״̬
	
	private int mCurState = STATE_NORMAL;
	private boolean isRecording = false;	// �Ѿ���ʼ¼��
	
	private DialogManager mDialogManager;
	
	private AudioManager mAudioManager;
	
	private float mTime;
	private boolean mReady;		// �Ƿ񴥷�longclick

	public AudioRecorderButton(Context context) {
		this(context, null);
	}

	public AudioRecorderButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mDialogManager = new DialogManager(getContext());
		
		String dir = Environment.getExternalStorageDirectory() + "/teuiqla_audios";
		mAudioManager = AudioManager.getInstance(dir);
		mAudioManager.setOnAudioStateListener(this);
		
		
		setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				
				mReady = true;
				mAudioManager.prepareAudio();
				return false;
			}
		});
	}
	
	/**
	 * 
	 * ¼����ɺ�Ļص�
	 * @author Tequila
	 * */
	public interface AudioFinishRecorderListener {
		
		void onFinish(float seconds, String filePath);
	}
	
	private AudioFinishRecorderListener mListener;
	
	public void setAudioFinishRecorderListener(AudioFinishRecorderListener listener) {
		
		mListener = listener;
	}
	/*
	 * ��ȡ������С��Runnable
	 * */
	private Runnable mGetVoiceLevelRunnable = new Runnable() {
		
		@Override
		public void run() {

			while (isRecording) {
				try {
					Thread.sleep(100);
					mTime += 0.1f;
					mHandler.sendEmptyMessage(MSG_VOICE_CHANGED);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	private static final int MSG_AUDIO_PREPARED = 0X110;
	private static final int MSG_VOICE_CHANGED = 0X111;
	private static final int MSG_DIALOG_DIMISS = 0X112;
	
	private Handler mHandler = new Handler() {
		
		public void handleMessage(android.os.Message msg) {
			
			switch (msg.what) {
			case MSG_AUDIO_PREPARED:
				
				//��ʾӦ����autio end prepared�Ժ�
				mDialogManager.showRecordingDialog();
				isRecording = true;
				new Thread(mGetVoiceLevelRunnable).start();
				
				break;
				
			case MSG_VOICE_CHANGED:
				
				mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
				
				break;
				
			case MSG_DIALOG_DIMISS:
	
				mDialogManager.dimissDialog();
				
				break;

			default:
				break;
			}
		};
	};
	
	@Override
	public void wellPrepared() {
		
		mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		int action = event.getAction();
		int x = (int) event.getX();
		int y = (int) event.getY();
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			
			changState(STATE_RECORDING);
			
			break;
			
		case MotionEvent.ACTION_MOVE:
			
			if (isRecording) {
				// ����x,y������,�ж��Ƿ���Ҫȡ��
				if (wantToCancel(x, y)) {
					changState(STATE_WANT_TO_CANCEL);
				} else {
					changState(STATE_RECORDING);
				}
			}
			
			break;
			
		case MotionEvent.ACTION_UP:
	
			if (!mReady) {
				reset();
				return super.onTouchEvent(event);
			} 
			
			if (!isRecording || mTime < 0.6f) {
				mDialogManager.tooShort();
				mAudioManager.cancel();
				mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DIMISS, 300);	// �ӳ�
			} else if (mCurState == STATE_RECORDING) {	// ����¼�ƽ���
				
				mDialogManager.dimissDialog();
				
				// release
				mAudioManager.release();
				
				// callbackToAct
				if (mListener != null) {
					mListener.onFinish(mTime, mAudioManager.getCurrentFilePath());
				}
				
			} else if (mCurState == STATE_WANT_TO_CANCEL){	
				
				mDialogManager.dimissDialog();
				mAudioManager.cancel();
				// cancel
			}
			
			reset();
			
			break;
			
		default:
			break;
		}
		
		return super.onTouchEvent(event);
	}

	/*
	 * �ָ�״̬����ʶλ
	 * */
	private void reset() {
		isRecording = false;
		mReady = false;
		mTime = 0;
		changState(STATE_NORMAL);
	}

	private boolean wantToCancel(int x, int y) {
		
		if (x < 0 || x > getWidth()) {	// �ж���ָ�ĺ������Ƿ񳬳���ť��Χ
			return true;
		}
		
		if (y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL) {
			return true;
		}
		
		return false;
	}

	private void changState(int state) {
		
		if (mCurState != state) {
			
			mCurState = state;
			
			switch (state) {
			case STATE_NORMAL:
				
				setBackgroundResource(R.drawable.btn_recorder_normal);
				setText(R.string.str_recorder_normal);
				break;
				
			case STATE_RECORDING:
				
				setBackgroundResource(R.drawable.btn_recording);
				setText(R.string.str_recorder_recording);
				
				if (isRecording) {
					mDialogManager.recording();
				}
				break;
				
			case STATE_WANT_TO_CANCEL:
	
				setBackgroundResource(R.drawable.btn_recording);
				setText(R.string.str_recorder_want_cancel);
				
				mDialogManager.wantToCancel();
				break;
				
			default:
				break;
			}
		}
		
	}
}
