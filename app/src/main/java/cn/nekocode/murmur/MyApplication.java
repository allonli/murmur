package cn.nekocode.murmur;

import android.app.Application;
import android.content.Context;

import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVOSCloud;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by nekocode on 2015/4/3 0003.
 */
public class MyApplication extends Application {
    public static MyApplication instant;

    @Override
    public void onCreate() {
        super.onCreate();
        instant = this;

        AVOSCloud.initialize(this, "njtyqtww55i0fikg9zgzuq5cayrbi7u85uiolfjoadch2pse", "ld1826dyuz53gxd4vx84j60lq9mg5860ksznirff41y2sau9");
        AVAnalytics.enableCrashReport(this, true);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Champagne & Limousines Bold.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
    }

    public static MyApplication get() {
        return instant;
    }

    public static Context getContext() {
        return instant.getApplicationContext();
    }
}
