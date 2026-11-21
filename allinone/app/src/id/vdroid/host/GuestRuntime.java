package id.vdroid.host;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import android.view.View;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * Resource proxy + classloader untuk satu instance.
 *
 * - AssetManager milik guest diberi path ke guest.apk (addAssetPath)
 * - Resources guest dipakai untuk inflate layout guest
 * - DexClassLoader memuat kode guest
 *
 * Ini resource proxy. v0.5 menambahkan Instrumentation hook yang membuat
 * sistem menjalankan Activity tamu (lihat GuestInstrumentation).
 */
public final class GuestRuntime {

    public final Resources resources;
    public final Context context;

    private final DexClassLoader loader;
    private final String dataDir;

    private GuestRuntime(Resources resources, Context context, DexClassLoader loader, String dataDir) {
        this.resources = resources;
        this.context = context;
        this.loader = loader;
        this.dataDir = dataDir;
    }

    public static GuestRuntime load(Context host, JSONObject inst) throws Exception {
        final String id = inst.getString("id");
        final File apk = Core.prepareGuestApk(host, id);

        final AssetManager am = AssetManager.class.getDeclaredConstructor().newInstance();
        Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
        Object cookie = addAssetPath.invoke(am, apk.getAbsolutePath());
        if (!(cookie instanceof Integer) || ((Integer) cookie) == 0) {
            throw new IOException("addAssetPath gagal untuk " + apk);
        }

        Resources base = host.getResources();
        final Resources guestRes = new Resources(
                am, base.getDisplayMetrics(), base.getConfiguration());

        ContextThemeWrapper guestCtx = new ContextThemeWrapper(host, 0) {
            @Override public Resources getResources() { return guestRes; }
            @Override public AssetManager getAssets() { return am; }
        };

        File odex = new File(Core.instanceDir(host, id), "odex");
        if (!odex.exists()) //noinspection ResultOfMethodCallIgnored
            odex.mkdirs();

        DexClassLoader loader = new DexClassLoader(
                apk.getAbsolutePath(),
                odex.getAbsolutePath(),
                null,
                GuestRuntime.class.getClassLoader());

        return new GuestRuntime(guestRes, guestCtx, loader,
                Core.instanceDir(host, id).getAbsolutePath());
    }

    public View createView(android.app.Activity host, JSONObject inst) throws Exception {
        Class<?> entry = Class.forName("id.vdroid.guest.GuestEntry", true, loader);
        Method create = entry.getMethod("create",
                Activity.class, Context.class, String.class, String.class);
        return (View) create.invoke(null, host, context, inst.toString(), dataDir);
    }

    public int classLoaderHash() {
        return loader.hashCode();
    }

    public int themeId() {
        return resources.getIdentifier("Theme_VD", "style", "id.vdroid.guest");
    }

    public Resources.Theme theme() {
        Resources.Theme t = resources.newTheme();
        t.setTo(context.getTheme());
        int id = themeId();
        if (id != 0) t.applyStyle(id, true);
        return t;
    }

    public DexClassLoader loader() { return loader; }
    public Context guestContext() { return context; }
    public String dataDir() { return dataDir; }
}
