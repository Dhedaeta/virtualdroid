package id.vdroid.cloud;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "vdcloud";

    private Prefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String server(Context c) { return p(c).getString("server", ""); }
    public static String user(Context c) { return p(c).getString("user", "admin"); }
    public static String pass(Context c) { return p(c).getString("pass", ""); }

    public static void save(Context c, String server, String user, String pass) {
        p(c).edit().putString("server", server)
                .putString("user", user)
                .putString("pass", pass).apply();
    }
}
