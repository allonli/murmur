package cn.nekocode.murmur.utils;

import cn.nekocode.murmur.MyApplication;

/**
 * Created by nekocode on 2015/4/22 0022.
 */
public class ToastUtils {
    public static void show(String str) {
        android.widget.Toast.makeText(MyApplication.get().getApplicationContext(), str, android.widget.Toast.LENGTH_SHORT).show();
    }
}
