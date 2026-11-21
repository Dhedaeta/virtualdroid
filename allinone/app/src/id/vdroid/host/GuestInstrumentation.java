package id.vdroid.host;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Instrumentation hook.
 *
 * ActivityThread tetap meluncurkan StubActivity (sesuai manifest host),
 * tetapi newActivity() kita mengembalikan instance Activity milik APK tamu.
 * callActivityOnCreate() mengganti base context/resources ke APK tamu
 * sebelum onCreate dijalankan.
 *
 * Ini yang membuat Activity tamu "benar-benar" dibuat oleh sistem, bukan
 * di-attach manual seperti v0.4.
 */
public class GuestInstrumentation extends Instrumentation {

    private static final String TAG = "VirtualDroid";

    private final Instrumentation delegate;
    private final Context appContext;
    private final Map<Activity, GuestRuntime> runtimes = new WeakHashMap<>();

    public GuestInstrumentation(Instrumentation delegate, Context appContext) {
        super();
        this.delegate = delegate;
        this.appContext = appContext;
        copyFields(delegate, this);
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (intent != null && intent.getBooleanExtra("vd_hook", false)) {
            String id = intent.getStringExtra("vd_instance");
            if (id != null) {
                try {
                    JSONObject inst = Core.find(appContext, id);
                    if (inst != null) {
                        GuestRuntime rt = GuestRuntime.load(appContext, inst);
                        String guestClass = intent.getStringExtra("vd_guest_class");
                        if (guestClass == null) {
                            guestClass = GuestManifest.launcher(appContext,
                                    new File(Core.instanceDir(appContext, id), "guest.apk"));
                        }
                        if (guestClass != null) {
                            Class<?> cls = Class.forName(guestClass, true, rt.loader());
                            try {
                                cls.getField("PROFILE_JSON").set(null, inst.toString());
                            } catch (Throwable ignored) {}
                            try {
                                cls.getField("DATA_DIR").set(null, rt.dataDir());
                            } catch (Throwable ignored) {}
                            try {
                                cls.getField("RES").set(null, rt.resources);
                            } catch (Throwable ignored) {}
                            try {
                                cls.getField("ASSETS").set(null, rt.context.getAssets());
                            } catch (Throwable ignored) {}
                            try {
                                cls.getField("THEME").set(null, rt.theme());
                            } catch (Throwable ignored) {}

                            Activity guest = (Activity) cls.newInstance();
                            runtimes.put(guest, rt);
                            Log.i(TAG, "newActivity guest=" + guestClass
                                    + " instance=" + id);
                            return guest;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "newActivity guest gagal, fallback stub", t);
                }
            }
        }
        return delegate.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        GuestRuntime rt = runtimes.get(activity);
        if (rt != null) {
            try {
                // attachBaseContext sudah dipanggil Activity.attach; ganti field-nya.
                Field base = ContextWrapper.class.getDeclaredField("mBase");
                base.setAccessible(true);
                base.set(activity, rt.context);

                Field res = ContextThemeWrapper.class.getDeclaredField("mResources");
                res.setAccessible(true);
                res.set(activity, rt.resources);

                int theme = rt.themeId();
                Resources.Theme guestTheme = rt.resources.newTheme();
                guestTheme.setTo(rt.context.getTheme());
                if (theme != 0) guestTheme.applyStyle(theme, true);

                // Ganti semua field Resources.Theme (mTheme) di hierarchy.
                Class<?> k = ContextThemeWrapper.class;
                while (k != null && k != Object.class) {
                    for (Field f : k.getDeclaredFields()) {
                        if (f.getType() == Resources.Theme.class) {
                            f.setAccessible(true);
                            f.set(activity, guestTheme);
                        }
                    }
                    k = k.getSuperclass();
                }
                if (theme != 0) activity.setTheme(theme);

                try {
                    int layoutId = rt.resources.getIdentifier(
                            "activity_guest", "layout", "id.vdroid.guest");
                    Log.i(TAG, "guest res same=" + (activity.getResources() == rt.resources)
                            + " layoutId=0x" + Integer.toHexString(layoutId)
                            + " base=" + activity.getBaseContext().getClass().getName());
                } catch (Throwable t) {
                    Log.e(TAG, "cek resource gagal", t);
                }

                Log.i(TAG, "guest context attached instance="
                        + activity.getIntent().getStringExtra("vd_instance"));
            } catch (Throwable t) {
                Log.e(TAG, "gagal swap context guest", t);
            }
        }
        super.callActivityOnCreate(activity, icicle);
    }

    private static void copyFields(Instrumentation from, Instrumentation to) {
        for (Field f : Instrumentation.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                f.set(to, f.get(from));
            } catch (Throwable ignored) {
                // field final / hidden; biarkan
            }
        }
    }
}
