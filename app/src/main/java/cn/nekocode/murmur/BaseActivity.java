package cn.nekocode.murmur;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class BaseActivity extends ActionBarActivity {
    private static ArrayList<Handler> handlers = new ArrayList<Handler>();
    protected MyHandler handler = new MyHandler(this);
    protected RequestQueue queue;

    public BaseActivity _this;

    public static void addHandler(Handler handler) {
        handlers.add(handler);
    }

    public static void deleteHandler(Handler handler) {
        handlers.remove(handler);
    }

    public static void removeAll() {
        handlers.clear();
    }

    public static void broadcast(Message message) {
        for (Handler handler : handlers) {
            Message msg = new Message();
            msg.copyFrom(message);
            handler.sendMessage(msg);
        }
    }

    public void sendMsg(Message message) {
        Message msg = new Message();
        msg.copyFrom(message);
        handler.sendMessage(msg);
    }

    public void sendMsgDelayed(Message message, int delayMillis) {
        Message msg = new Message();
        msg.copyFrom(message);
        handler.sendMessageDelayed(msg, delayMillis);
    }

    public void runDelayed(Runnable runnable, int delayMillis) {
        Message msg = new Message();
        msg.what = -101;
        msg.arg1 = -102;
        msg.arg2 = -103;
        msg.obj = runnable;
        handler.sendMessageDelayed(msg, delayMillis);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _this = this;
        queue = Volley.newRequestQueue(this);
        addHandler(handler);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
//        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
//        MobclickAgent.onPause(this);
    }

    @Override
    public void finish() {
        deleteHandler(handler);
        super.finish();
    }

    @Override
    protected void onDestroy() {
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        super.onDestroy();
    }

    static class MyHandler extends Handler {
        private WeakReference<BaseActivity> mOuter;

        public MyHandler(BaseActivity activity) {
            mOuter = new WeakReference<BaseActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final BaseActivity outer = mOuter.get();
            if (outer == null) {
                return;
            }

            if(msg.what==-101 && msg.arg1==-102 && msg.arg2==-103) {
                ((Runnable) msg.obj).run();
                return;
            }

            outer.handler(msg);
        }
    }

    public void handler(Message msg) {

    }
}
