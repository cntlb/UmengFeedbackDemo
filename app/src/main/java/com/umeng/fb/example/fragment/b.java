//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.umeng.fb.example.fragment;
//package com.umeng.fb.image;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.umeng.fb.example.CustomConversationActivity;
import com.umeng.fb.fragment.FeedbackFragment;
import com.umeng.fb.util.c;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class b {
    private static final String a = b.class.getName();

    public b() {
    }

    public static Bitmap a(String var0, int var1) {
        Options var2 = new Options();
        var2.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(var0, var2);
        var2.inSampleSize = a(var2, var1);
        var2.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(var0, var2);
    }

    public static int a(Options var0, int var1) {
        int var2 = var0.outHeight;
        int var3 = var0.outWidth;
        float var4 = (float)var1 * 0.36F;
        return (int)(var2 > var3?(float)var2 / var4:(float)var3 / var4);
    }

    private static void b(Bitmap var0) {
        if(var0 != null && !var0.isRecycled()) {
            var0.recycle();
            var0 = null;
        }

    }

    public static void a(final Context context, final Uri uri, final String ruuid) {
        (new AsyncTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(Void... var1x) {
                // 压缩图像, 得到缩略图
                return Boolean.valueOf(b.c(context, uri, ruuid));
            }

            protected void onPostExecute(Boolean var1x) {
                if(var1x.booleanValue()) {
                    // 反馈图片
                    ((CustomConversationActivity)context).sendImage(ruuid);
                }
            }
        }).execute(new Void[0]);
    }

    private static boolean c(Context context, Uri uri, String ruuid) {
        boolean var3 = true;
        String imagePath = c.b(context, ruuid);;
        File file = new File(imagePath);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        FileOutputStream var6 = null;
        Bitmap var7 = null;

        try {
            var7 = a(b(context, uri));
            var6 = new FileOutputStream(file);
            if(var7 != null && var7.compress(CompressFormat.JPEG, 80, var6)) {
                ;
            }
        } catch (Exception var17) {
            file.delete();
            var17.printStackTrace();
            var3 = false;
        } finally {
            b(var7);

            try {
                var6.close();
            } catch (IOException var16) {
                var16.printStackTrace();
            }

        }

        return var3;
    }

    public static Bitmap a(Bitmap var0) {
        if(var0 == null) {
            return null;
        } else {
            int var1 = var0.getHeight();
            int var2 = var0.getRowBytes() * var0.getHeight() / 1024 / 1024;
            if(var2 > 15) {
                Matrix var3 = new Matrix();
                var3.postScale(0.5F, 0.5F);
                Bitmap var4 = Bitmap.createBitmap(var0, 0, 0, var0.getWidth(), var1, var3, true);
                b(var0);
                return var4;
            } else {
                return var0;
            }
        }
    }

    private static synchronized Bitmap b(Context var0, Uri var1) throws IOException {
        ContentResolver var2 = var0.getContentResolver();
        InputStream var3 = var2.openInputStream(var1);
        Options var4 = new Options();
        var4.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(var3, (Rect)null, var4);
        var3.close();
        if(var4.outWidth != -1 && var4.outHeight != -1) {
            int var5 = var4.outHeight > var4.outWidth?var4.outHeight:var4.outWidth;
            int var6 = a(var0);
            int var7 = var5 > var6?var5 / var6:1;
            var4.inJustDecodeBounds = false;
            var4.inSampleSize = var7;
            var3 = var2.openInputStream(var1);
            Bitmap var8 = BitmapFactory.decodeStream(var3, (Rect)null, var4);
            var3.close();
            return var8;
        } else {
            return null;
        }
    }

    private static int a(Context var0) {
        DisplayMetrics var1 = new DisplayMetrics();
        WindowManager var2 = (WindowManager)((WindowManager)var0.getSystemService(Context.WINDOW_SERVICE));
        var2.getDefaultDisplay().getMetrics(var1);
        return var1.heightPixels > var1.widthPixels?var1.heightPixels:var1.widthPixels;
    }

    public static boolean a(Context var0, Uri var1) {
        ContentResolver var2 = var0.getContentResolver();
        Bitmap var3 = null;

        boolean var5;
        try {
            InputStream var4 = var2.openInputStream(var1);
            Options var12 = new Options();
            var12.inJustDecodeBounds = false;
            var12.inSampleSize = 4;
            var3 = BitmapFactory.decodeStream(var4, (Rect)null, var12);
            var4.close();
            if(var12.outWidth == -1 || var12.outHeight == -1) {
                boolean var6 = false;
                return var6;
            }

            return true;
        } catch (Exception var10) {
            var5 = false;
        } finally {
            b(var3);
        }

        return var5;
    }
}
