package com.umeng.fb.example;

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
import com.umeng.fb.fragment.FeedbackFragment;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;
import com.umeng.fb.model.Store;
import com.umeng.fb.model.UserInfo;
import com.umeng.fb.net.a;
import com.umeng.fb.push.FeedbackPush;
import com.umeng.message.PushAgent;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    FeedbackFragment feedbackFragment;

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
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:

                        break;
                     case MotionEvent.ACTION_UP:

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

        feedbackFragment = FeedbackFragment.newInstance(conversation_id);
        getSupportFragmentManager().beginTransaction().add(R.id.container, feedbackFragment).commit();

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
            String[] var4 = data.getDataString().split("/");
            Log.i(TAG, "data.getDataString -- " + data.getDataString());

//            FeedbackFragment feedbackFragment = FeedbackFragment.newInstance(conversation_id);
            try {
                Method method = FeedbackFragment.class.getDeclaredMethod("b");
                method.setAccessible(true);
                method.invoke(feedbackFragment);

                Field field = FeedbackFragment.class.getDeclaredField("n");
                field.setAccessible(true);
                field.set(feedbackFragment, conversation);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(com.umeng.fb.image.b.a(this, data.getData())) {
                com.umeng.fb.image.b.a(this, data.getData(), uuid);//执行异步任务后,做的就是下面的事情
                //conversation.addUserReply("", k(), "image_reply", -1.0F);

            } else {
                //Toast.makeText(this, textViewg.B(this.mContext), 0).show();
            }
        }
    }

    private String k() {
        return "R" + UUID.randomUUID().toString();
    }

    // 发送图片
    public void sendImage(View view) {
    }


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

    public void recordAudio() {
        audioAgent = AudioAgent.getInstance(this);
        boolean hasInitial = audioAgent.recordStart(uuid);
        if(hasInitial){

        }
    }



    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

        }
    };

    private Timer timer;
    private void trySendAudio() {
        if(this.timer != null) {
            this.timer.cancel();
        }

        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            int a = 10;// 发送倒计时

            public void run() {
                if (FeedbackFragment.this.dialog.isShowing()) {
                    if (this.a > 0) {
                        FeedbackFragment.this.sendMessage(3, this.a);
                        --this.a;
                    } else {
                        FeedbackFragment.this.sendMessage(2);//反馈声音操作(发送? 取消?)
                        FeedbackFragment.this.T = false;
                        this.cancel();
                    }
                }

            }
        }, 51000L, 1000L);// 5秒后每隔1s执行一次从   10开始倒计时
    }
}
