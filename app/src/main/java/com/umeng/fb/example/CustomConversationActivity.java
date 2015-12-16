package com.umeng.fb.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

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

import java.util.List;
import java.util.UUID;

public class CustomConversationActivity extends Activity {
    private static final String TAG = CustomConversationActivity.class.getSimpleName();
    private static final int REQUEST_FOR_IMAGE = 100;
    // -----------Views------------------
    private Spinner spinner;
    private EditText infoEditText;
    private ImageView imageView;
    private EditText info;

    //------------umeng--------------
    private FeedbackAgent feedbackAgent;
    private AudioAgent audioAgent;
    private Conversation conversation;
    private FeedbackPush feedbackPush;
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

        feedbackAgent = new FeedbackAgent(this);
        feedbackAgent.sync();
        feedbackAgent.openAudioFeedback();
        feedbackAgent.openFeedbackPush();

        String conversation_id = getIntent().getStringExtra(FeedbackFragment.BUNDLE_KEY_CONVERSATION_ID);
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
        if(resultCode == -1 && requestCode == 1 && data != null) {
            String[] var4 = data.getDataString().split("/");
            Log.i(TAG, "data.getDataString -- " + data.getDataString());

            if(com.umeng.fb.image.b.a(this, data.getData())) {
                com.umeng.fb.image.b.a(this, data.getData(), k());
                conversation.addUserReply("", conversation.getId(), "image_reply", -1.0F);
                refresh();
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
}
