package com.umeng.fb.example.feedback;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.SyncListener;
import com.umeng.fb.audio.AudioAgent;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Store;
import com.umeng.fb.model.UserInfo;
import com.umeng.fb.net.a;
import com.umeng.fb.push.FeedbackPush;
import com.umeng.message.PushAgent;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by 汤林冰 on 2015/12/19 11:06.
 */
public class FeedbackUtil {

    public static final int REQUEST_FOR_IMAGE = 100;
    public static final int AUDIO_INNITIALIZED_FAILED = -0x1;
    public static final int AUDIO_RECORDING = 0x1;

    ///////////////////////////////////////////////////////////////////////////
    // Nested Classes
    ///////////////////////////////////////////////////////////////////////////
    public static class AudioInitialFailedException extends Exception{
        public AudioInitialFailedException() {
            super("初始化失败!");
        }
    }

     public static class IllegalImageScaleException extends Exception{
        public IllegalImageScaleException() {
            super("图片格式非法!");
        }
    }

    //------------------------fields start-----------------
    private static final String TAG = FeedbackUtil.class.getSimpleName();
    private static FeedbackUtil feedbackUtil;
    private Activity mActivity;
    private FeedbackAgent feedbackAgent;
    private Conversation conversation;
    private SyncListener syncListener;
    private String conversation_id;
    private FeedbackPush feedbackPush;
    private String uuid;
    private AudioAgent audioAgent;
    //-----------fields end---------------------------


    private FeedbackUtil(Activity context) {
        this.mActivity = context;
    }

    public static FeedbackUtil getInstance(Activity activity) {
        if(feedbackUtil == null){
            feedbackUtil = new FeedbackUtil(activity);
        }
        return feedbackUtil;
    }

    /**初始化 umeng 反馈配置*/
    public void initConfig(){
        feedbackAgent = new FeedbackAgent(mActivity);
        feedbackAgent.sync();
        feedbackAgent.openAudioFeedback();
        feedbackAgent.openFeedbackPush();
        PushAgent.getInstance(mActivity).setDebugMode(true);
        PushAgent.getInstance(mActivity).enable();

        conversation = feedbackAgent.getDefaultConversation();
        conversation_id = conversation.getId();
        uuid = randomID();
        feedbackPush = FeedbackPush.getInstance(mActivity);
        feedbackPush.setConversationId(conversation_id);
    }

    /** update user info
     *
     * @param userInfo {@link UserInfo}对象, 具有以下格式:
                        {
                           age_group  : group(int),
                           gender     : 性别(default="") ,
                           contact    :
                                    {
                                      QQ    : qqnum,
                                      phone : phonenum,
                                      email: email,
                                       ...
                                    },
                           remark     :
                                    {
                                      key:value
                                    }
                         }
     */
    public void updateUserInfo2UmengServer(UserInfo userInfo) {
        // 启用反馈后, 以下代码可以运行: 注意模拟器失败!
        Store.getInstance(mActivity).saveUserInfo(userInfo);
        new Thread(new Runnable() {
            public void run() {
                Store store = Store.getInstance(mActivity);
                JSONObject jsonObject = store.getUserInfo().toJson();
                // uid怎么生成?
                boolean result = new a(mActivity).a(jsonObject);
                Log.i(TAG, "update result:" + result);
            }
        }).start();
    }

    /**
     * 请求码{@link #REQUEST_FOR_IMAGE}
     * */
    public void requestLocalImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mActivity.startActivityForResult(intent, REQUEST_FOR_IMAGE);
    }

    /**
     * 请求码{@link #REQUEST_FOR_IMAGE}<br/>
     * 反馈后自动调用 {@link Conversation#sync(SyncListener)}
     * */
    public void sendImage2UmengServer(Uri imageUri) throws IllegalImageScaleException {
        //获取图片缩略图大小, 读取成功
        boolean sendable = com.umeng.fb.image.b.a(mActivity, imageUri);
        if(!sendable){
            throw new IllegalImageScaleException();
        }

        final String imageUUID = randomID();
        // 这里改写了 com.umeng.fb.image.b.a 方法
        b.a(mActivity, imageUri, imageUUID, new b.OnPostExecute() {
            @Override
            public void onPost(Boolean success) {
                if(success){
                    conversation.addUserReply("", imageUUID, "image_reply", -1.0F);
                    refresh();
                }else{
                    Toast.makeText(mActivity, "图片读取失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 发送文字反馈信息<br/>
     * 反馈后自动调用 {@link Conversation#sync(SyncListener)}
     * */
    public void sendText2UmengServer(CharSequence charSequence) {
        conversation.addUserReply(charSequence.toString());
        refresh();
    }


    ///////////////////////////////////////////////////////////////////////////
    // 录音
    ///////////////////////////////////////////////////////////////////////////

    /*package*/ Handler audioHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case AUDIO_INNITIALIZED_FAILED:
                    recordListener.onStart(false);
                    break;
                case AUDIO_RECORDING:
                    recordListener.onRecording(msg.arg1);
                    break;
            }
        }
    };

    /**
     * 录制声音回调抽象类
     */
    public abstract class OnAudioRecordListener {
        /**
         * isInitialSuccess=false, 录音初始化失败
         */
        public abstract void onStart(boolean isInitialSuccess);

        /**
         * 声音录制中
         * @param voiceLevel 返回当前声音的级别 1~7
         */
        public abstract void onRecording(int voiceLevel);

        /**
         * 执行取消录音操作, 由调用 {@link #recordCancel()}触发
         */
        public abstract void onCancel();

        /**
         * 正常录音下停止录音 {@link #recordStop()} 将自动反馈到 umeng服务器并刷新会话
         */
        public void onSendAudio() {
            conversation.addUserReply("", uuid, "audio_reply", audioAgent.getAudioDuration());
            refresh();
        }

        /**
         * 释放录音失败返回
         */
        public abstract void onError();
    }

    private OnAudioRecordListener recordListener;

    /**
     * 设置录音回调
     * @param recordListener
     */
    public void setOnRecordListener(OnAudioRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    /**
     * 录音初始化, 结果传递给 {@link OnAudioRecordListener#onStart(boolean)}
     */
    public void recordStart(){
        audioAgent = AudioAgent.getInstance(mActivity);
        uuid = randomID();
        try {
            recordListener.onStart(new ProxyRecord(mActivity,audioAgent, this).startRecord(uuid));
        } catch (IllegalAccessException e) {
            recordListener.onStart(false);
        }
    }

    /**
     * 取消录音,删除缓存文件, 触发 {@link OnAudioRecordListener#onCancel()}
     */
    public void recordCancel(){
        audioAgent.recordShortStop();
        //删除录音文件, 存储路径: getFilesDir().getAbsolutePath() + "/umeng/fb/audio/" + uuid + ".opus"
        com.umeng.fb.util.c.a(mActivity, uuid);
        recordListener.onCancel();
    }

    /**
     * 停止录音(不同于取消!), 将语音反馈给服务器
     * @seealso  {@link OnAudioRecordListener#onSendAudio()}
     */
    public void recordStop(){
        if (audioAgent.getRecordStatus()) {
            int var1 = audioAgent.recordStop();
            if (var1 > 0) {
                recordListener.onSendAudio();
                return;
            }
        }
        recordListener.onError();
    }

    // 刷新会话
    private void refresh() {
        conversation.sync(syncListener);
    }

    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    public Conversation getConversation() {
        return conversation;
    }

    //生成随机id
    private String randomID() {
        return "R" + UUID.randomUUID().toString();
    }
}
