package id.vdroid.host;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;

/** Memasang GuestInstrumentation ke ActivityThread (hidden API). */
public final class InstrumentationHook {

    private static final String TAG = "VirtualDroid";
    private static boolean installed;

    private InstrumentationHook() {}

    public static synchronized boolean install(Context context) {
        if (installed) return true;
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            if (thread == null) throw new IllegalStateException("ActivityThread null");

            Field f = at.getDeclaredField("mInstrumentation");
            f.setAccessible(true);
            Instrumentation old = (Instrumentation) f.get(thread);
            if (old instanceof GuestInstrumentation) {
                installed = true;
                return true;
            }
            f.set(thread, new GuestInstrumentation(old, context.getApplicationContext()));
            installed = true;
            Log.i(TAG, "Instrumentation hook terpasang");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Instrumentation hook gagal", t);
            return false;
        }
    }

    public static boolean isInstalled() { return installed; }
}
