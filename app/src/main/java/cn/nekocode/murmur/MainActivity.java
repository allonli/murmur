package cn.nekocode.murmur;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.SaveCallback;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cn.nekocode.murmur.beans.MurmurBean;
import cn.nekocode.murmur.beans.SongBean;
import cn.nekocode.murmur.media.Player;
import cn.nekocode.murmur.net.API;
import cn.nekocode.murmur.utils.FileManager;
import cn.nekocode.murmur.utils.ImageUtils;
import cn.nekocode.murmur.utils.LruImageCache;
import cn.nekocode.murmur.utils.MyCallback;
import cn.nekocode.murmur.utils.PlayManager;
import cn.nekocode.murmur.utils.PxUtils;
import cn.nekocode.murmur.utils.ToastUtils;
import cn.nekocode.murmur.widgets.ShaderRenderer;


public class MainActivity extends BaseActivity implements View.OnTouchListener {
    private static final int MSG_NEED_UPDATE = 1304;
    private static final int MSG_TIME_COUNT = 1305;
    private static final int ANIMATION_DURATION = 500;

    @InjectView(R.id.surfaceview)
    GLSurfaceView surfaceView;
    @InjectView(R.id.titleTextView)
    TextView titleTextView;
    @InjectView(R.id.performerTextView)
    TextView performerTextView;
    @InjectView(R.id.modelTextView)
    TextView topicTextView;
    @InjectView(R.id.timeTextView)
    TextView timeTextView;
    @InjectView(R.id.coverImageView)
    ImageSwitcher coverImageView;
    @InjectView(R.id.relativeLayout)
    RelativeLayout relativeLayout;
    @InjectView(R.id.progressWheel)
    ProgressWheel progressWheel;
    @InjectView(R.id.linearLayout)
    LinearLayout linearLayout;

    private Map<String, MurmurBean> murmurs;
    private List<SongBean> songs;
    private GestureDetector gestureDetector = null;


    private ValueAnimator backgroundColorAnimation;
    private ValueAnimator textColorAnimation;

    private ShaderRenderer renderer;

    private PlayManager playManager;
    private int nowPlayPosition = -1;
    private String murmursToPlay[] = null;

    private boolean wantUpdate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        wantUpdate = true;

        FileManager.createAppRootDirs();
        playManager = PlayManager.getInstant();
        playManager.setPlayerListener(new Player.PlayerListener() {
            @Override
            public void onPrepared() {
                progressWheel.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onBuffering(int bufferingProgress) {
            }

            @Override
            public void onProgress(int duration, int position) {
            }
        });

        AVInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            public void done(AVException e) {
                if (e == null) {
                    // 保存成功
                    String installationId = AVInstallation.getCurrentInstallation().getInstallationId();
                    // 关联  installationId 到用户表等操作……
                } else {
                    // 保存失败，输出错误信息
                }
            }
        });
        PushService.setDefaultPushCallback(this, BaseActivity.class);

        API.loadSetting(new MyCallback.Callback1<JSONObject>() {
            @Override
            public Object run(JSONObject param) {
                try {
                    String topic = param.getString("topic");
                    if (!topicTextView.getText().toString().equals(topic))
                        topicTextView.setText(topic);

                    if (wantUpdate) {
                        String lastestVersion = param.getString("lastest_version");
                        String lastestDownloadAddress = param.getString("lastest_download_address");

                        String nowVersion = _this.getPackageManager().getPackageInfo(_this.getPackageName(), 0).versionName;
                        if (!lastestVersion.equals(nowVersion)) {
                            Message message = new Message();
                            message.what = MSG_NEED_UPDATE;
                            message.obj = lastestDownloadAddress;
                            _this.sendMsg(message);
                        }

                        wantUpdate = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        setupGLSufaceview();
        setupHeader();

        timeCounter.run();
    }

    private long time = -1;
    private Runnable timeCounter = new Runnable() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = MSG_TIME_COUNT;
            sendMsgDelayed(message, 1000);

            time++;
            long second = time % 60;
            long minute = time / 60;
            timeTextView.setText(minute + ":" + second);
            renderer.setSpeed(0.9f + minute * 0.04f);
        }
    };

    private void showTip() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tip, null);
        new AlertDialog.Builder(this).setView(dialogView).create().show();
    }

    private void playMurmur() {
        if (FileManager.isMurmurBuffered() && murmursToPlay != null) {
            playManager.addMurmurs(murmursToPlay);
        }
    }

    private void setupGLSufaceview() {
        gestureDetector = new GestureDetector(this, new GestureListener());

        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setOnTouchListener(this);

        String shader;
        try {
            Resources r = getResources();
            InputStream is = r.openRawResource(R.raw.shader);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i = is.read();
            while (i != -1) {
                baos.write(i);
                i = is.read();
            }

            shader = baos.toString();
            is.close();
        } catch (IOException e) {
            shader = "";
        }


        renderer = new ShaderRenderer(this, shader);
        renderer.setBackColor(getResources().getColor(R.color.main));
        surfaceView.setRenderer(renderer);
    }

    private void setupHeader() {
        if (!FileManager.isMurmurBuffered()) {
            API.getMurmurs(new MyCallback.Callback1<Map<String, MurmurBean>>() {
                @Override
                public Object run(Map<String, MurmurBean> param) {
                    murmurs = param;
                    FileManager.bufferMurmur(_this, murmurs, new MyCallback.Callback0() {
                        @Override
                        public Object run() {
                            playMurmur();
                            nowPlayPosition = 0;
                            loadSong();
                            showTip();
                            return null;
                        }
                    });
                    return null;
                }
            });
        }

        if (murmursToPlay == null) {
            API.getPlayMurmurs(new MyCallback.Callback1<String[]>() {
                @Override
                public Object run(String[] param) {
                    murmursToPlay = param;
                    playMurmur();
                    return null;
                }
            });
        }

        API.getSongs(new MyCallback.Callback1<List<SongBean>>() {
            @Override
            public Object run(List<SongBean> param) {
                songs = param;

                nowPlayPosition = 0;
                loadSong();
                return null;
            }
        });

        coverImageView.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView i = new ImageView(_this);
                i.setScaleType(ImageView.ScaleType.FIT_CENTER);
                i.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.FILL_PARENT));
                return i;
            }
        });

        Animation fadein = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadein.setDuration(ANIMATION_DURATION);
        coverImageView.setInAnimation(fadein);

        Animation fadeout = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        fadeout.setDuration(ANIMATION_DURATION);
        coverImageView.setOutAnimation(fadeout);

        coverImageView.setImageResource(R.drawable.transparent);

        oldBackgroundColor = getResources().getColor(R.color.main);
        oldTextColor = 0xffffffff;
    }

    private void loadSong() {
        if (!FileManager.isMurmurBuffered() || nowPlayPosition < 0)
            return;

        SongBean songBean = songs.get(nowPlayPosition);
        progressWheel.setVisibility(View.VISIBLE);

        titleTextView.setText(songBean.getName());
        performerTextView.setText("- " + songBean.getPerformer());

        playManager.playSong(songBean.getUrl());

        getCover(songBean.getCoverUrl());
    }

    private void getCover(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        ImageLoader imageLoader = new ImageLoader(queue, LruImageCache.getLruImageCache());
        imageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ToastUtils.show("can not load song cover.");
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap bitmap = response.getBitmap();
                if (bitmap != null) {
                    changeViewsColor(bitmap);

                    Bitmap circleBitmap = ImageUtils.getCircleBitmap(bitmap);
                    Drawable drawable = ImageUtils.bitmap2Drawable(circleBitmap);
                    coverImageView.setImageDrawable(drawable);
                }
            }
        });
    }

    private int oldBackgroundColor = 0;
    private int oldTextColor = 0;

    private void changeViewsColor(Bitmap bitmap) {
        Palette.generateAsync(bitmap,
                new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        Palette.Swatch swatch = null;
                        while (swatch == null) {
                            swatch = palette.getDarkVibrantSwatch();
                            if (swatch != null)
                                break;

                            swatch = palette.getVibrantSwatch();
                            if (swatch != null)
                                break;

                            swatch = palette.getDarkMutedSwatch();
                            if (swatch != null)
                                break;

                            swatch = palette.getLightMutedSwatch();
                            if (swatch != null)
                                break;
                        }

                        if (swatch != null) {
                            backgroundColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                                    oldBackgroundColor, swatch.getRgb()).setDuration(ANIMATION_DURATION + 300);
                            backgroundColorAnimation.setInterpolator(new LinearInterpolator());
                            backgroundColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    relativeLayout.setBackgroundColor((Integer) animation.getAnimatedValue());
                                    renderer.setBackColor((Integer) animation.getAnimatedValue());
                                    if (Build.VERSION.SDK_INT >= 21) {
                                        getWindow().setStatusBarColor((Integer) animation.getAnimatedValue());
                                    }

                                    oldBackgroundColor = (Integer) animation.getAnimatedValue();
                                }
                            });
                            backgroundColorAnimation.start();


                            textColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                                    oldTextColor, swatch.getTitleTextColor()).setDuration(ANIMATION_DURATION + 300);
                            textColorAnimation.setInterpolator(new LinearInterpolator());
                            textColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    progressWheel.setBarColor(
                                            (Integer) animation.getAnimatedValue());
                                    titleTextView.setTextColor(
                                            (Integer) animation.getAnimatedValue());
                                    performerTextView.setTextColor(
                                            (Integer) animation.getAnimatedValue());
                                    topicTextView.setTextColor(
                                            (Integer) animation.getAnimatedValue());
                                    timeTextView.setTextColor(
                                            (Integer) animation.getAnimatedValue());
                                    oldTextColor = (Integer) animation.getAnimatedValue();
                                }
                            });
                            textColorAnimation.start();
                        }
                    }
                });
    }

    private void update(final String address) {
        new AlertDialog.Builder(this).setMessage("A newer version of murmur! is available,do u want to update?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setPositiveButton("Sure", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FileManager.update(_this, address);
            }
        }).setCancelable(false).create().show();
    }

    @Override
    public void handler(Message msg) {
        switch (msg.what) {
            case MSG_NEED_UPDATE:
                String downloadAddress = (String) msg.obj;
                update(downloadAddress);
                break;
            case MSG_TIME_COUNT:
                timeCounter.run();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setMessage("Are you sure to exit?").
                setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setPositiveButton("Sure", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _this.finish();
            }
        }).create().show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        playManager.onDestory();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    class GestureListener implements GestureDetector.OnGestureListener {
        private int FLING_MIN_DISTANCE = PxUtils.dip2px(MyApplication.get().getApplicationContext(), 100);
        private int FLING_MIN_DISTANCE_Y = PxUtils.dip2px(MyApplication.get().getApplicationContext(), 150);
        private int FLING_MIN_VELOCITY = 1;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        private long lastestTapTime = 0;

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            long nowTapTime = System.currentTimeMillis();
            if (nowTapTime - lastestTapTime < 800) {
//                ToastUtils.show("double tap");
//
//                float nowX = linearLayout.getX();
//                float nowY = linearLayout.getY();
//                float targetX = PxUtils.dip2px(_this, 62);
//                float targetY = 0f - PxUtils.dip2px(_this, 20);

//                ValueAnimator transXAnimation = ValueAnimator.ofFloat(nowY, targetY).setDuration(ANIMATION_DURATION);
//                transXAnimation.setInterpolator(new DecelerateInterpolator());
//                transXAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animation) {
//                        linearLayout.setY((Float) animation.getAnimatedValue());
//                    }
//                });
//                transXAnimation.start();
//
//                ValueAnimator transYAnimation = ValueAnimator.ofFloat(nowX, targetX).setDuration(ANIMATION_DURATION);
//                transYAnimation.setInterpolator(new DecelerateInterpolator());
//                transYAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animation) {
//                        linearLayout.setX((Float) animation.getAnimatedValue());
//                        linearLayout.requestLayout();
//                    }
//                });
//                transYAnimation.start();

                lastestTapTime = 0;
                return false;
            }

            lastestTapTime = nowTapTime;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            if (Math.abs(e1.getY() - e2.getY()) > FLING_MIN_DISTANCE_Y)
                return false;

            if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                //向右滑动
                nowPlayPosition++;
                if (nowPlayPosition >= songs.size()) {
                    nowPlayPosition = 0;
                }
                loadSong();

            } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                //向左滑动
                nowPlayPosition--;
                if (nowPlayPosition < 0) {
                    nowPlayPosition = songs.size() - 1;
                }
                loadSong();
            }

            return false;
        }
    }
}
