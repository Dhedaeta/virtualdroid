package id.vdroid.host;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;

/**
 * Manifest parser minimal untuk APK tamu.
 * Memakai PackageManager.getPackageArchiveInfo (tanpa menginstal APK).
 */
public final class GuestManifest {

    private GuestManifest() {}

    public static PackageInfo parse(Context c, File apk) {
        try {
            PackageManager pm = c.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(
                    apk.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (pi != null && pi.applicationInfo != null) {
                pi.applicationInfo.sourceDir = apk.getAbsolutePath();
                pi.applicationInfo.publicSourceDir = apk.getAbsolutePath();
            }
            return pi;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Activity pertama yang diekspor; untuk guest sample: GuestActivity. */
    public static String launcher(Context c, File apk) {
        PackageInfo pi = parse(c, apk);
        if (pi == null || pi.activities == null || pi.activities.length == 0) return null;
        String pkg = pi.packageName;
        for (ActivityInfo ai : pi.activities) {
            if (ai != null && ai.exported && ai.name != null) return full(ai.name, pkg);
        }
        ActivityInfo ai = pi.activities[0];
        return ai == null || ai.name == null ? null : full(ai.name, pkg);
    }

    public static String label(Context c, File apk) {
        PackageInfo pi = parse(c, apk);
        if (pi == null || pi.applicationInfo == null) return null;
        try {
            return c.getPackageManager().getApplicationLabel(pi.applicationInfo).toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static int activityCount(Context c, File apk) {
        PackageInfo pi = parse(c, apk);
        return pi == null || pi.activities == null ? 0 : pi.activities.length;
    }

    private static String full(String name, String pkg) {
        if (name.startsWith(".")) return pkg + name;
        if (name.indexOf('.') < 0) return pkg + "." + name;
        return name;
    }
}
