package com.cookee.tools;

import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.IOException;

public class AudioManager {

  private static final String TAG = "AudioManager";

  public interface OnRecordListener {
    void onStart();

    void onVoiceLevelChange(int level);

    void onFinish();
  }

  private MediaRecorder mRecorder;
  private boolean mIsRecording = false;
  private String mSavePath;
  private int mMaxVoiceLevel;
  private OnRecordListener mRecordListener;

  private AudioManager() {

  }

  private static AudioManager mInstance;

  public static AudioManager getInstance() {// single instance mode
    if (mInstance == null) {
      synchronized (AudioManager.class) {
        if (mInstance == null) {
          mInstance = new AudioManager();
        }
      }
    }
    return mInstance;
  }

  public void prepare(String saveDir, int maxVoiceLevel, OnRecordListener l) {
    mRecorder = new MediaRecorder();
    this.mMaxVoiceLevel = maxVoiceLevel;//设置录音级别
    mRecordListener = l;

    File file = new File(saveDir);//设置录音文件的保存路径
    if (file != null && !file.exists()) {
      file.mkdirs();
    }
    file = new File(saveDir, System.currentTimeMillis() + ".aac");
    mSavePath = file.getAbsolutePath();//省去了不同操作系统文件分隔符的书写麻烦

    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //音频源麦克风
    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);//输出格式
    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//编码格式 .acc文件
    mRecorder.setOutputFile(mSavePath);//音频文件存储位置
    try {
      mRecorder.prepare();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    mRecorder.start();   // Recording prepareed
    mIsRecording = true;
    if (l != null) {
      l.onStart();
      mHandler.post(mRunnable);
    }
  }

  private Runnable mRunnable = new Runnable() {
    @Override
    public void run() {
      int voiceLevel = getVoiceLevel(mMaxVoiceLevel);
      Message msg = Message.obtain(mHandler);
      msg.arg1 = voiceLevel;
      msg.sendToTarget();
    }
  };

  private Handler mHandler = new Handler() {//handle record operate
    @Override
    public void handleMessage(Message msg) {
      if (mRecordListener != null) {
        mRecordListener.onVoiceLevelChange(msg.arg1);
      }
      if (mIsRecording) {
        mHandler.postDelayed(mRunnable, 300);//every 300 ms refresh data(voice level)
      } else {
        removeCallbacks(mRunnable);
        if (mRecordListener != null) {
          mRecordListener.onFinish();
        }
      }
    }
  };

  public void release() {//release all source which can release
    if (mRecorder == null) {
      return;
    }

    if (mIsRecording) {
      mRecorder.stop();
    }
    mRecorder.release();
    mIsRecording = false;
    mRecorder = null;
  }

  private int getVoiceLevel(int maxLevel) {
    if (mIsRecording) {
      try {
        // mMediaRecorder.getMaxAmplitude()音频的振幅范围:1~32767
        return maxLevel * mRecorder.getMaxAmplitude() / 32768 + 1;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return 1;
  }

  public void cancel() {//取消音频：释放资源+删除音频文件
    if (mRecorder != null) {
      release();
    }

    File file = new File(mSavePath);
    if (file != null && file.exists()) {
      file.delete();
    }
  }

  public String getSavePath() {
    return mSavePath;
  }
}
