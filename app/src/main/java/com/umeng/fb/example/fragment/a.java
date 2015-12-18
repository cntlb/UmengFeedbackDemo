package com.umeng.fb.example.fragment;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umeng.fb.audio.AudioAgent;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;

import java.util.List;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import com.umeng.fb.audio.AudioAgent;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;
import com.umeng.fb.model.Conversation.OnChangeListener;
import com.umeng.fb.res.e;
import com.umeng.fb.res.f;
import com.umeng.fb.res.g;
import com.umeng.fb.res.h;
import java.util.List;

//ReplyAdapter
public class a extends BaseAdapter {
    private final String c = a.class.getName();
    private LayoutInflater d;
    private AnimationDrawable e;
    private Conversation f;
    private Context g;
    private AudioAgent h;
    private com.umeng.fb.image.a i;
    private Dialog j;
    public static final int a = 0;
    public static final int b = 1;
    private final int k = 3;
    private final int l = 0;
    private final int m = 1;
    private final int n = 2;
    private static Handler o;

    public a(Context var1, Conversation var2) {
        this.g = var1;
        this.d = LayoutInflater.from(this.g);
        this.i = com.umeng.fb.image.a.a();
        this.b();
        this.f = var2;
        this.f.setOnChangeListener(new Conversation.OnChangeListener() {
            public void onChange() {
                a.this.notifyDataSetChanged();
            }
        });
    }

    public int getCount() {
        List var1 = this.f.getReplyList();
        return var1 == null?0:var1.size();
    }

    public View getView(int var1, View var2, ViewGroup var3) {
        Object var4 = null;
        Reply var5 = (Reply)this.f.getReplyList().get(var1);
        if(var2 == null) {
            if("text_reply".equals(var5.content_type)) {
                var2 = this.d.inflate(f.b(this.g), (ViewGroup)null);
                var4 = new a.d(null);
                var2.setTag(var4);
                ((a.b)var4).a(var2);
            } else if("audio_reply".equals(var5.content_type)) {
                var2 = this.d.inflate(f.c(this.g), (ViewGroup)null);
                var4 = new a.a(null);
                var2.setTag(var4);
                ((a.b)var4).a(var2);
            } else {
                var2 = this.d.inflate(f.d(this.g), (ViewGroup)null);
                var4 = new a.c(null);
                var2.setTag(var4);
                ((a.b)var4).a(var2);
            }
        } else {
            var4 = (a.b)var2.getTag();
        }

        ((a.b)var4).a(var5);
        if(var1 + 1 < this.getCount()) {
            Reply var6 = (Reply)this.f.getReplyList().get(var1 + 1);
            boolean var7 = "new_feedback".equals(var5.type) && "user_reply".equals(var6.type);
            var7 |= var6.type.equals(var5.type);
            var7 |= var1 + 1 == this.getCount();
            if(var7) {
                ((a.b)var4).g.setVisibility(8);
            }
        }

        return var2;
    }

    public Object getItem(int var1) {
        return this.f.getReplyList().get(var1);
    }

    public long getItemId(int var1) {
        return (long)var1;
    }

    public int getViewTypeCount() {
        return 3;
    }

    public int getItemViewType(int var1) {
        Reply var2 = (Reply)this.f.getReplyList().get(var1);
        // TODO getItemViewType     text_reply-0
        return "text_reply".equals(var2.content_type)?0:("audio_reply".equals(var2.content_type)?1:2);
    }

    private void b() {
        o = new Handler() {
            public void handleMessage(Message var1) {
                switch(var1.what) {
                    case 0:
                        a.this.c();
                        if(a.this.h != null && a.this.h.getPlayStatus()) {
                            a.this.h.stopPlayer();
                        }
                        break;
                    case 1:
                        a.this.notifyDataSetChanged();
                }

            }
        };
    }

    private void c() {
        if(this.e != null && this.e.isRunning()) {
            this.e.stop();
            this.e.selectDrawable(0);
        }

    }

    private int a(Context var1, int var2) {
        int var3 = this.a(var1);
        byte var4 = 100;
        int var5 = var4 + var2 * var3 / 80;
        if((double)var5 > (double)var3 * 0.7D) {
            var5 = (int)((double)var3 * 0.7D);
        }

        return var5;
    }

    private int a(Context var1) {
        DisplayMetrics var2 = new DisplayMetrics();
        WindowManager var3 = (WindowManager)((WindowManager)var1.getSystemService("window"));
        var3.getDefaultDisplay().getMetrics(var2);
        return var2.widthPixels > var2.heightPixels?var2.heightPixels:var2.widthPixels;
    }

    public static Handler a() {
        return o;
    }

    private void a(String var1) {
        if(this.j == null) {
            this.j = new Dialog(this.g, 16973831);
            this.j.setContentView(f.m(this.g));
            this.j.getWindow().setWindowAnimations(h.b(this.g));
        }

        ImageView var2 = (ImageView)this.j.findViewById(e.C(this.g));
        Bitmap var3 = BitmapFactory.decodeFile(com.umeng.fb.util.c.b(this.g, var1));
        var2.setImageBitmap(var3);
        this.j.show();
        var2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View var1) {
                a.this.j.dismiss();
            }
        });
    }

    private class c extends a.b {
        ImageView a;

        private c() {
            super();
        }

        public void onClick(View var1) {
            super.onClick(var1);
            if(var1 == this.a) {
                a.this.a(this.i.reply_id);
            }

        }

        public void a(View var1) {
            super.a(var1);
            this.a = (ImageView)var1.findViewById(e.B(a.this.g));
            this.a.setOnClickListener(this);
        }

        public void a(Reply var1) {
            super.a(var1);
            a.this.i.a(com.umeng.fb.util.c.b(a.this.g, var1.reply_id), this.a, a.this.a(a.this.g));
        }
    }

    private class a extends a.b {
        View a;
        View b;
        TextView c;

        private a() {
            super();
        }

        public void onClick(View var1) {
            super.onClick(var1);
            if(var1 == this.a) {
                if(a.this.h == null) {
                    a.this.h = AudioAgent.getInstance(a.this.g);
                }

                a.this.c();
                AnimationDrawable var2 = (AnimationDrawable)this.b.getBackground();
                if(a.this.h.getPlayStatus()) {
                    a.this.h.stopPlayer();
                    if(a.this.e != null && var2.equals(a.this.e)) {
                        return;
                    }
                }

                a.this.e = var2;
                a.this.e.start();
                a.this.h.startPlay(this.i.reply_id);
            }

        }

        public void a(View var1) {
            super.a(var1);
            this.a = var1.findViewById(e.y(a.this.g));
            this.b = var1.findViewById(e.z(a.this.g));
            this.c = (TextView)var1.findViewById(e.A(a.this.g));
            this.a.setOnClickListener(this);
        }

        public void a(Reply var1) {
            super.a(var1);
            this.c.setText((int)var1.audio_duration + "\"");
            RelativeLayout.LayoutParams var2 = new RelativeLayout.LayoutParams(a.this.a(a.this.g, (int)var1.audio_duration), -2);
            this.a.setLayoutParams(var2);
            if(com.umeng.fb.common.b.a(a.this.g).d()) {
                ;
            }
        }
    }

    private class d extends a.b {
        TextView a;

        private d() {
            super();
        }

        public void a(View var1) {
            super.a(var1);
            this.a = (TextView)var1.findViewById(e.b(a.this.g));
        }

        public void a(Reply var1) {
            super.a(var1);
            this.a.setText(var1.content);
        }
    }

    private class b implements View.OnClickListener {
        TextView e;
        View f;
        View g;
        ImageView h;
        Reply i;

        private b() {
        }

        public void onClick(View var1) {
            if(var1 == this.h) {
                a.this.f.sendReplyOnlyOne(a.this.f.getId(), this.i);
            }

        }

        public void a(View var1) {
            this.e = (TextView)var1.findViewById(e.e(a.this.g));
            this.g = var1.findViewById(e.i(a.this.g));
            this.f = var1.findViewById(e.p(a.this.g));
            this.h = (ImageView)var1.findViewById(e.q(a.this.g));
            this.h.setOnClickListener(this);
            this.h.setClickable(true);
        }

        public void a(Reply var1) {
            this.i = var1;
            if("dev_reply".equals(var1.type)) {
                this.f.setBackgroundColor(a.this.g.getResources().getColor(com.umeng.fb.res.c.a(a.this.g)));
                this.e.setText(com.umeng.fb.util.d.a(a.this.g, var1.created_at));
            } else {
                this.f.setBackgroundColor(a.this.g.getResources().getColor(com.umeng.fb.res.c.c(a.this.g)));
                if("not_sent".equals(var1.status)) {
                    this.e.setText(g.d(a.this.g));
                    this.h.setImageResource(com.umeng.fb.res.d.a(a.this.g));
                    this.h.setAnimation((Animation)null);
                    this.h.setVisibility(0);
                    this.h.setClickable(true);
                } else if(!"sending".equals(var1.status) && !"will_sent".equals(var1.status)) {
                    this.e.setText(com.umeng.fb.util.d.a(a.this.g, var1.created_at));
                    this.h.setAnimation((Animation)null);
                    this.h.setVisibility(8);
                    this.h.setClickable(false);
                } else {
                    this.e.setText(g.e(a.this.g));
                    this.h.setImageResource(com.umeng.fb.res.d.a(a.this.g));
                    this.h.setVisibility(0);
                    RotateAnimation var2 = new RotateAnimation(0.0F, -360.0F, 1, 0.5F, 1, 0.5F);
                    var2.setInterpolator(new LinearInterpolator());
                    var2.setRepeatCount(-1);
                    var2.setDuration(700L);
                    this.h.startAnimation(var2);
                    this.h.setClickable(false);
                }
            }

            this.g.setVisibility(0);
        }
    }
}
