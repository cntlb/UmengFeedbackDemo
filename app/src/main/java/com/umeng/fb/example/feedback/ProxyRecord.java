package com.umeng.fb.example.feedback;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.umeng.fb.audio.AudioAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by 汤林冰 on 2015/12/19 16:38.
 */
/*package*/ class ProxyRecord {

    private static final String TAG = "ProxyRecord";
    private long recordLength = 0L;
    private boolean isRecording = false;
    private int bufferSize;
    private Context context;
    private AudioRecord audioRecord;
    private FeedbackUtil util;

    private AudioAgent audioAgent;//用于反射jar包中的AudioAgent.class成员, 实例化之
    private Class<AudioAgent> aClass = AudioAgent.class;
    private Field field_b_long_g;//录音长度, com.umeng.fb.audio.b的字段
    private Field field_b_AudioRecord_c;
    private Field field_b_int_b;

    private Field field_AudioAgent_String_l;
    private Field field_AudioAgent_b_h;
    private String pathRaw; //音频存储路径
    public ProxyRecord(Context context, AudioAgent audioAgent, FeedbackUtil util) {
        this.context = context;
        this.audioAgent = audioAgent;
        this.util = util;
        try {
            openAccess();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openAccess() throws Exception {
        field_AudioAgent_String_l = getField(AudioAgent.class, "l");
        field_AudioAgent_b_h = getField(AudioAgent.class, "h");

        field_b_int_b = getField(com.umeng.fb.audio.b.class, "b");
        field_b_AudioRecord_c = getField(com.umeng.fb.audio.b.class, "c");
        field_b_long_g = getField(com.umeng.fb.audio.b.class, "g");
    }

    private static Field getField(Class cls, String name) throws NoSuchFieldException {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /**
     * {@link AudioAgent#recordStart(String)}
     * @param uuid
     */
    public boolean startRecord(final String uuid) throws IllegalAccessException {
        //1.创建音频文件缓存目录:
        final String path = context.getFilesDir().getAbsolutePath() + "/umeng/fb/audio/cache/";
        File file = new File(path);
        if(!file.exists()){
            boolean mkdirs = file.mkdirs();
            if(!mkdirs){
                return  false;
            }
        }
        //this.l = var1;
        field_AudioAgent_String_l.set(audioAgent, uuid);
        // 创建两个文件路径:

        pathRaw = path + uuid + ".raw";
        final String pathWav = path + uuid + ".wav";// 貌似没用到

        //2.计算最小缓冲大小，单位为字节:
        bufferSize = 2 * AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //          this.b = 2 * AudioRecord.getMinBufferSize(16000, 16, 2);
        field_b_int_b.setInt(field_AudioAgent_b_h.get(audioAgent), bufferSize);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        //          b.this.c = new AudioRecord(1, 16000, 16, 2, this.b);
        field_b_AudioRecord_c.set(field_AudioAgent_b_h.get(audioAgent), audioRecord);
        audioRecord = (AudioRecord) field_b_AudioRecord_c.get(field_AudioAgent_b_h.get(audioAgent));

        if(audioRecord == null || audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED){
            Message.obtain(util.audioHandler, FeedbackUtil.AUDIO_INNITIALIZED_FAILED).sendToTarget();
            return false;
        }

        //3. 如果STATE_INITIALIZED成功, 下面开始录音:

        recordLength = 0;
        // b.this.g = 0L
        field_b_long_g.setLong(field_AudioAgent_b_h.get(audioAgent), recordLength);//h.g = recordLength
        isRecording = true;

        //4. 开线程录音
        new Thread(runnable).start();

        return true;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (audioRecord != null) {

                byte[] buffer = new byte[bufferSize];
                FileOutputStream out = null;

                try {
                    File fileRaw = new File(pathRaw);
                    if (fileRaw.exists()) {
                        fileRaw.delete();
                    }
                    out = new FileOutputStream(fileRaw);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 使用 AudioRecord 录制音频
                audioRecord.startRecording();

                long time = SystemClock.elapsedRealtime();
                while (isRecording) {
                    // 实际读取的数据长度，一般小于buffersize
                    int length = audioRecord.read(buffer, 0, bufferSize);
                    if (AudioRecord.ERROR_INVALID_OPERATION == length) {
                        break;
                    }

                    //每隔300ms获取一次音量, 时间不够可以调少
                    if(SystemClock.elapsedRealtime() - time > 300){
                        int level = getVoiceLevel(buffer, length);
                        Message msg = Message.obtain();
                        msg.what = FeedbackUtil.AUDIO_RECORDING;
                        msg.arg1 = level;
                        util.audioHandler.sendMessage(msg);
                        time = SystemClock.elapsedRealtime();
                    }

                    try {
                        out.write(buffer);
                        recordLength += bufferSize;
                        field_b_long_g.setLong(field_AudioAgent_b_h.get(audioAgent), recordLength);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                try {
                    release();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    // release resource
    private void release() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        isRecording = false;
        if (audioRecord != null) {
            if (AudioRecord.RECORDSTATE_STOPPED == audioRecord.getRecordingState()) {
                // handle AudioRecord RECORD STOPPED      we do nothing
            }
            audioRecord = null;

            Method stop = AudioRecord.class.getMethod("stop");
            Method release = AudioRecord.class.getMethod("release");
            //AudioAgent.b.AudioRecord.stop()
            Object object = field_AudioAgent_b_h.get(audioAgent);
            Object receiver = field_b_AudioRecord_c.get(object);
            Log.i(TAG, "object:"+object);
            Log.i(TAG, "receiver:"+receiver);
            // FIXME java.lang.NullPointerException: expected receiver of type android.media.AudioRecord, but got null
            stop.invoke(receiver, new Object[0]);
            release.invoke(receiver, new Object[0]);
            field_b_AudioRecord_c.set(field_AudioAgent_b_h.get(audioAgent), null);
        }
    }

    private static final double BASE_LEVEL = 1;
    private static final double MAX_VOLUME = 10 * Math.log10(127*127/BASE_LEVEL);
    private static final int MAX_LEVEL = 7;
    private static int getVoiceLevel(byte[] buffer, int length){
        long squareSum = 0;
        // 将 buffer 内容取出，求平方和
        for (int i = 0; i < buffer.length; i++) {
            squareSum += buffer[i] * buffer[i];
        }
        // 平方和求平均值，计算得到音量大小。
        double mean = squareSum / (double) length;
        double volume = 10 * Math.log10(mean/BASE_LEVEL);
        Log.d(TAG, "分贝值:" + volume);
        return (int) (1+ (volume-1)*MAX_LEVEL/MAX_VOLUME);// 1~7
    }
}
