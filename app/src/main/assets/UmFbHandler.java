package com.cookee.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import com.cookee.tools.MyLog;
import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.SyncListener;
import com.umeng.fb.audio.AudioAgent;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by Lyon on 12/18/15.
 */
public class UmFbHandler {

  private static final String UMENG_FB_IMAGE_PATH = "/umeng/fb/image/%s.jpg";
  private Context mContext;
  private Conversation mConversation;
  private FeedbackAgent mFeedbackAgent;
  private AudioAgent mAudioAgent;
  private String mAudioTag;
  private boolean mIsDisposed;
  private SyncListener mSyncListener;

  public UmFbHandler(Context context) {
    mContext = context.getApplicationContext();
    FeedbackAgent.setDebug(false);
    mFeedbackAgent = new FeedbackAgent(mContext);
    mConversation = mFeedbackAgent.getDefaultConversation();

    mAudioAgent = AudioAgent.getInstance(mContext);
  }

  public void dispose() {
    mIsDisposed = true;
    mContext = null;
    mConversation = null;
  }

  public FeedbackAgent getFeedbackAgent() {
    return mFeedbackAgent;
  }

  public Conversation getConversation() {
    return mConversation;
  }

  public void setSyncListener(SyncListener listener) {
    mSyncListener = listener;
  }

  private void sync() {
    mConversation.sync(mSyncListener);
  }

  public void addTextReply(String text) {
    mConversation.addUserReply(text);
    sync();
  }

  public void addImageReply(InputStream imageStream) {
    new AddImageReplyTask().execute(imageStream);
  }

  public void addImageReply(Uri uri) throws FileNotFoundException {
    new AddImageReplyTask().execute(mContext.getContentResolver().openInputStream(uri));
  }

  public void startRecordAudio() {
    mAudioTag = UUID.randomUUID().toString();
    mAudioAgent.recordStart(mAudioTag);
  }

  public boolean finishRecordAudio() {
    if (mAudioTag == null) {
      return false;
    }
    if (!mAudioAgent.getAudioInitStatus()) {
      return false;
    }
    if (mAudioAgent.getAudioDuration() < 0.5f) {
      return false;
    }
    if (!mAudioAgent.getRecordStatus()) {
      return false;
    }
    int r = mAudioAgent.recordStop();
    if (r <= 0) {
      return false;
    }
    mConversation.addUserReply("", mAudioTag, Reply.CONTENT_TYPE_AUDIO_REPLY,
        mAudioAgent.getAudioDuration());
    sync();
    mAudioTag = null;
    return true;
  }

  private class AddImageReplyTask extends AsyncTask<Object, Object, Boolean> {

    private String mImageTag = UUID.randomUUID().toString();

    @Override
    protected Boolean doInBackground(Object... params) {
      InputStream is = (InputStream) params[0];
      try {
        Bitmap bmp = BitmapFactory.decodeStream(is);
        if (mIsDisposed) {
          return false;
        }
        File f = makeImagePath(mImageTag);
        f.getParentFile().mkdirs();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80,
            new FileOutputStream(f));
        if (mIsDisposed) {
          return false;
        }
        return true;
      } catch (FileNotFoundException e) {
        //return; TODO notify maybe?
        MyLog.d("cookee", e.getLocalizedMessage());
      } finally {
        IoUtils.closeQuietly(is);
      }
      return false;
    }

    @Override
    protected void onPostExecute(Boolean isOk) {
      if (isOk) {
        mConversation.addUserReply("", mImageTag, Reply.CONTENT_TYPE_IMAGE_REPLY, -1);
        sync();
      }
    }
  }

  private File makeImagePath(String tag) {
    return new File(mContext.getFilesDir(), String.format(UMENG_FB_IMAGE_PATH, tag));
  }

}
