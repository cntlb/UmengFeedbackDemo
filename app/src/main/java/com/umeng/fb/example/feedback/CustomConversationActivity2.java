package com.umeng.fb.example.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.umeng.fb.SyncListener;
import com.umeng.fb.audio.AudioAgent;
import com.umeng.fb.example.R;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;
import com.umeng.fb.model.UserInfo;

import java.util.List;

public class CustomConversationActivity2 extends FragmentActivity {
    private static final String TAG = CustomConversationActivity2.class.getSimpleName();
    // -----------Views------------------
    private Spinner spinner;
    private EditText infoEditText;
    private ImageView imageView;
    private EditText info;
    private TextView deadTime;//倒计时
    private Button record;

    //------------umeng--------------
    private AudioAgent audioAgent;
    private Conversation conversation;
    private String uuid;
    FeedbackUtil feedbackUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_conversation);
        initConfig();
        initViewsAndEvent();
    }

    private void initConfig() {
        feedbackUtil = FeedbackUtil.getInstance(this);
        feedbackUtil.initConfig();
        conversation = feedbackUtil.getConversation();
        feedbackUtil.setSyncListener(syncListener);
        refresh();
    }

    private void initViewsAndEvent() {
        // 用户信息
        spinner = (Spinner) findViewById(R.id.infoSpinner);
        infoEditText = (EditText) findViewById(R.id.infoEditText);
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启用反馈后, 以下代码可以运行: 注意模拟器失败!
                String key = (String) spinner.getSelectedItem();
                String value = infoEditText.getText().toString();
                UserInfo userInfo = new UserInfo();
                userInfo.getContact().put(key, value);
                feedbackUtil.updateUserInfo2UmengServer(userInfo);
            }
        });
        //文字
        findViewById(R.id.sendText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackUtil.sendText2UmengServer(info.getText());
            }
        });

        // 发送图片
        imageView = (ImageView) findViewById(R.id.image);
        info = (EditText) findViewById(R.id.info);
        findViewById(R.id.pickImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackUtil.requestLocalImage();
            }
        });

        //语音
        deadTime = (TextView) findViewById(R.id.deadTime);
        record = (Button) findViewById(R.id.record);
        record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        record.setText("松开发送");
                        feedbackUtil.recordStart();
                        break;
                    case MotionEvent.ACTION_UP:
                        record.setText("按下录音");
                        feedbackUtil.recordStop();
                        break;
                }
                return true;
            }
        });

        feedbackUtil.setOnRecordListener(new MyAudioListener(feedbackUtil));
    }

    private class MyAudioListener extends FeedbackUtil.OnAudioRecordListener{
        //*****成员内部类的实例化必须借助外部类!
        public MyAudioListener(FeedbackUtil feedbackUtil) {
            feedbackUtil.super();
        }

        @Override
        public void onStart(boolean isInitialSuccess) {
            Log.i(TAG, String.format("onStart(%s)", isInitialSuccess));
        }

        @Override
        public void onRecording(int voiceLevel) {
            Log.i(TAG, String.format("onRecording(%d)", voiceLevel));
            deadTime.setText(String.valueOf(voiceLevel));
        }

        @Override
        public void onCancel() {
            Log.i(TAG, String.format("onCancel()"));
        }

        @Override
        public void onError() {
            Log.i(TAG, "onError");
        }

        @Override
        public void onSendAudio() {
            super.onSendAudio();
            Log.i(TAG, "onSendAudio");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FeedbackUtil.REQUEST_FOR_IMAGE && data != null) {
            try {
                feedbackUtil.sendImage2UmengServer(data.getData());
            } catch (FeedbackUtil.IllegalImageScaleException e) {
                e.printStackTrace();
            }
        }
    }

    private SyncListener syncListener = new SyncListener() {
        @Override
        public void onReceiveDevReply(List<Reply> list) {
            Log.i(TAG, "onReceiveDevReply--->" + list.toString());
        }

        @Override
        public void onSendUserReply(List<Reply> list) {
            Log.i(TAG, "onSendUserReply--->" + list.toString());
        }
    };
    // 刷新会话
    public void refresh() {
        conversation.sync(syncListener);
    }
}
