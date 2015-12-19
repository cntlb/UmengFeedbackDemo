package com.umeng.fb.example.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.SyncListener;
import com.umeng.fb.audio.AudioAgent;
import com.umeng.fb.example.R;
import com.umeng.fb.fragment.FeedbackFragment;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;
import com.umeng.fb.model.Store;
import com.umeng.fb.model.UserInfo;
import com.umeng.fb.net.a;
import com.umeng.fb.push.FeedbackPush;
import com.umeng.message.PushAgent;

import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class CustomConversationActivity extends FragmentActivity {
    private static final String TAG = CustomConversationActivity.class.getSimpleName();
    private static final int REQUEST_FOR_IMAGE = 100;
    // -----------Views------------------
    private Spinner spinner;
    private EditText infoEditText;
    private ImageView imageView;
    private EditText info;
    private TextView deadTime;//倒计时
    private Button record;

    //------------umeng--------------
    private FeedbackAgent feedbackAgent;
    private AudioAgent audioAgent;
    private Conversation conversation;
    private FeedbackPush feedbackPush;
    private String conversation_id;
    private String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_conversation);
        // 用户信息
        spinner = (Spinner) findViewById(R.id.infoSpinner);
        infoEditText = (EditText) findViewById(R.id.infoEditText);
        // 发送图片
        imageView = (ImageView) findViewById(R.id.image);
        info = (EditText) findViewById(R.id.info);
        //语音
        deadTime = (TextView) findViewById(R.id.deadTime);
        record = (Button) findViewById(R.id.record);
        record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        recordAudio();
                        break;
                    case MotionEvent.ACTION_UP:
                        sending();
                        if (audioAgent.getRecordStatus()) {
                            int var1 = audioAgent.recordStop();
                            if (var1 > 0) {
                                sendAudio();
                            }
                        }
                        break;
                }
                return true;
            }
        });

        feedbackAgent = new FeedbackAgent(this);
        feedbackAgent.sync();
        feedbackAgent.openAudioFeedback();
        feedbackAgent.openFeedbackPush();

        conversation_id = getIntent().getStringExtra(FeedbackFragment.BUNDLE_KEY_CONVERSATION_ID);
        uuid = k();
        feedbackPush = FeedbackPush.getInstance(this);
        feedbackPush.setConversationId(conversation_id);

        conversation = feedbackAgent.getDefaultConversation();
        refresh();
        PushAgent.getInstance(this).setDebugMode(true);
        PushAgent.getInstance(this).enable();

    }

    // update user info
    public void saveUserInfo(View view) {
        // 启用反馈后, 以下代码可以运行: 注意模拟器失败!
        String key = (String) spinner.getSelectedItem();
        String value = infoEditText.getText().toString();

        UserInfo userInfo = new UserInfo();
        userInfo.getContact().put(key, value);
        Store.getInstance(this).saveUserInfo(userInfo);

        new Thread(new Runnable() {
            public void run() {
                Store store = Store.getInstance(CustomConversationActivity.this);
                JSONObject jsonObject = store.getUserInfo().toJson();
                // uid怎么生成?
                boolean result = new a(CustomConversationActivity.this).a(jsonObject);
                Log.i(TAG, "update result:"+result);
            }
        }).start();
    }



    // 挑选图片
    public void pickImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_FOR_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == -1 && requestCode == REQUEST_FOR_IMAGE && data != null) {
            if(com.umeng.fb.image.b.a(this, data.getData())) {
                // TODO 这里改写了 com.umeng.fb.image.b.a 方法
                b.a(this, data.getData(), k(), null);//执行异步任务后,做的就是下面的事情
            } else {
                //Toast.makeText(this, textViewg.B(this.mContext), 0).show();
            }
        }
    }

    //生成随机文件名
    private String k() {
        return "R" + UUID.randomUUID().toString();
    }

    // 刷新会话
    public void refresh() {
        conversation.sync(new SyncListener() {
            @Override
            public void onReceiveDevReply(List<Reply> list) {
                Log.i(TAG, "onReceiveDevReply--->"+list.toString());
            }

            @Override
            public void onSendUserReply(List<Reply> list) {
                Log.i(TAG, "onSendUserReply--->"+list.toString());
            }
        });
    }

    public void sendText(View view) {
        conversation.addUserReply(info.getText().toString());
        refresh();
    }

    public void sendImage(String uuid){
        conversation.addUserReply("", uuid, "image_reply", -1.0F);
        refresh();
    }

    ///////////////////////////////////////////////////////////////////////////
    // 录音
    ///////////////////////////////////////////////////////////////////////////
    public void recordAudio() {
        audioAgent = AudioAgent.getInstance(this);
        uuid = k();
        boolean hasInitial = audioAgent.recordStart(uuid);
        Log.i(TAG, "hasInitial --->" + hasInitial);
        if(hasInitial){

        }
    }

    private void sendAudio() {
        conversation.addUserReply("", uuid, "audio_reply", audioAgent.getAudioDuration());
        refresh();
    }


    private static final int FEEDBACK_AUDIO_SENDING = 0x1;
    private static final int FEEDBACK_AUDIO_COMPLETE = 0x2;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case FEEDBACK_AUDIO_SENDING:
                    deadTime.setText(String.format("%ds", msg.arg1));
                    break;
                case FEEDBACK_AUDIO_COMPLETE:
//                    if(audioAgent.getRecordStatus()) {
//                        int var1 = audioAgent.recordStop();
//                        if (var1 > 0) {
//                           sendAudio();
//                        }
//                    }
                    break;
            }
        }
    };


    private Timer timer;
    private void sending() {
        if(this.timer != null) {
            this.timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            int a = 10;// 发送倒计时
            public void run() {
                    if (this.a > 0) {
                        Message.obtain(mHandler, FEEDBACK_AUDIO_SENDING, a, 0).sendToTarget();
                        --this.a;
                    } else {
                        Message.obtain(mHandler, FEEDBACK_AUDIO_COMPLETE).sendToTarget();
                        this.cancel();
                    }
            }
        }, 51000L, 1000L);// 5秒后每隔1s执行一次从   10开始倒计时
    }
}
