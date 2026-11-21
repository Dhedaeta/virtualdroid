package id.vdroid.host;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * EKSPERIMENTAL: menjalankan Activity milik APK tamu di dalam StubActivity.
 *
 * Ini bukan hook ActivityThread (sistem tetap meluncurkan StubActivity).
 * Kita membuat instance GuestActivity, memanggil Activity.attach() dengan
 * token window host, lalu onCreate/onStart/onResume, dan menempelkan decor
 * view-nya ke container host.
 *
 * Kalau reflection diblokir (hidden API) atau signature attach berubah,
 * StubActivity jatuh ke mode resource proxy.
 */
public final class ActivityHijack {

    private ActivityHijack() {}

    public static View attach(Activity host, JSONObject inst, GuestRuntime rt) throws Exception {
        Class<?> cls = Class.forName("id.vdroid.guest.GuestActivity", true, rt.loader());

        // override statis agar Activity tamu memakai profil & data instance
        cls.getField("PROFILE_JSON").set(null, inst.toString());
        cls.getField("DATA_DIR").set(null, rt.dataDir());

        Activity guest = (Activity) cls.newInstance();

        Class<?> atClass = Class.forName("android.app.ActivityThread");
        Object at = atClass.getMethod("currentActivityThread").invoke(null);
        if (at == null) throw new IllegalStateException("ActivityThread null");
        Object instr = atClass.getMethod("getInstrumentation").invoke(at);

        ActivityInfo ai = new ActivityInfo();
        ai.packageName = "id.vdroid.guest";
        ai.name = "id.vdroid.guest.GuestActivity";
        ai.processName = host.getPackageName();
        ai.applicationInfo = host.getApplicationInfo();
        ai.exported = true;
        int theme = rt.resources.getIdentifier("Theme_VD", "style", "id.vdroid.guest");
        if (theme != 0) ai.theme = theme;

        Method attach = null;
        for (Method m : Activity.class.getDeclaredMethods()) {
            if ("attach".equals(m.getName())
                    && m.getParameterCount() >= 10
                    && m.getParameterTypes()[0] == Context.class) {
                if (attach == null || m.getParameterCount() > attach.getParameterCount()) {
                    attach = m;
                }
            }
        }
        if (attach == null) throw new NoSuchMethodException("Activity.attach");
        attach.setAccessible(true);

        Class<?>[] types = attach.getParameterTypes();
        Object[] args = new Object[types.length];
        IBinder windowToken = host.getWindow().getDecorView().getWindowToken();
        boolean tokenUsed = false;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == Context.class) {
                args[i] = rt.context;
            } else if (t == Instrumentation.class) {
                args[i] = instr;
            } else if (t == IBinder.class) {
                args[i] = tokenUsed ? null : windowToken;
                tokenUsed = true;
            } else if (t == int.class) {
                args[i] = 0;
            } else if (t == Application.class) {
                args[i] = host.getApplication();
            } else if (t == Intent.class) {
                Intent it = new Intent();
                it.setComponent(new ComponentName("id.vdroid.guest",
                        "id.vdroid.guest.GuestActivity"));
                args[i] = it;
            } else if (t == ActivityInfo.class) {
                args[i] = ai;
            } else if (t == CharSequence.class) {
                args[i] = "Guest";
            } else if (t == Configuration.class) {
                args[i] = host.getResources().getConfiguration();
            } else if (t.getName().equals("android.app.ActivityThread")) {
                args[i] = at;
            } else {
                args[i] = null;
            }
        }

        attach.invoke(guest, args);
        lifecycle(guest, "onCreate", new Class<?>[]{Bundle.class}, new Object[]{null});
        lifecycle(guest, "onStart", new Class<?>[0], new Object[0]);
        lifecycle(guest, "onResume", new Class<?>[0], new Object[0]);

        View decor = guest.getWindow().getDecorView();
        decor.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return decor;
    }

    private static void lifecycle(Activity a, String name, Class<?>[] sig, Object[] args)
            throws Exception {
        Method m = Activity.class.getDeclaredMethod(name, sig);
        m.setAccessible(true);
        m.invoke(a, args);
    }
}
