package id.vdroid.host;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Penyimpanan instance + generator identitas.
 *
 * Catatan: ini bagian yang NYATA. IMEI dibuat valid Luhn, Android ID/MAC/GAID
 * acak per instance, dan tiap instance punya direktori sendiri.
 */
public final class Core {

    public static final String PREFS = "vdroid";
    public static final String KEY_INSTANCES = "instances";
    public static final String ASSET_GUEST = "guest.apk";

    /** brand, model, codename, chipset, android, sdk, ram(GB), storage(GB) */
    public static final String[][] PRESETS = {
            {"Google",  "Pixel 8a",          "akita",  "Google Tensor G3",      "16", "36", "4", "64"},
            {"Xiaomi",  "Xiaomi 14",         "houji",  "Snapdragon 8 Gen 3",    "16", "36", "4", "128"},
            {"Samsung", "Galaxy S24",        "e1q",    "Exynos 2400",           "16", "36", "6", "128"},
            {"OPPO",    "Reno 12 5G",        "cp9",    "Dimensity 7300",        "16", "36", "4", "128"},
            {"Infinix", "Note 40 Pro",       "x6851",  "Helio G99 Ultimate",    "16", "36", "3", "64"}
    };

    private static final SecureRandom RNG = new SecureRandom();

    private Core() {}

    public static JSONArray load(Context c) {
        String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_INSTANCES, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void save(Context c, JSONArray arr) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_INSTANCES, arr.toString()).apply();
    }

    public static JSONObject find(Context c, String id) {
        JSONArray arr = load(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    public static JSONObject create(Context c, String[] p) {
        JSONArray arr = load(c);
        int n = arr.length() + 1;
        JSONObject o = new JSONObject();
        try {
            o.put("id", "vd_" + hex(6).toLowerCase(Locale.US));
            o.put("name", "HP Virtual " + n);
            o.put("brand", p[0]);
            o.put("model", p[1]);
            o.put("code", p[2]);
            o.put("chip", p[3]);
            o.put("android", p[4]);
            o.put("sdk", Integer.parseInt(p[5]));
            o.put("ram", Integer.parseInt(p[6]));
            o.put("storage", Integer.parseInt(p[7]));
            o.put("root", "magisk");
            o.put("imei", newImei(p[0]));
            o.put("androidId", hex(16).toUpperCase(Locale.US));
            o.put("serial", p[0].substring(0, 2).toUpperCase(Locale.US) + digits(9));
            o.put("mac", newMac());
            o.put("gaid", java.util.UUID.randomUUID().toString());
            o.put("created", System.currentTimeMillis());
            o.put("fingerprint", fingerprint(o));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        arr.put(o);
        save(c, arr);
        try { prepareGuestApk(c, o.getString("id")); } catch (Exception ignored) {}
        return o;
    }

    public static void delete(Context c, String id) {
        JSONArray arr = load(c);
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) out.put(o);
        }
        save(c, out);
        deleteRecursive(instanceDir(c, id));
    }

    public static File instanceDir(Context c, String id) {
        File f = new File(c.getFilesDir(), "instances/" + id);
        if (!f.exists()) //noinspection ResultOfMethodCallIgnored
            f.mkdirs();
        return f;
    }

    /**
     * Menyalin APK tamu dari assets ke ruang instance.
     * Android 14+ menolak DEX dari file yang writable, jadi file dibuat read-only.
     */
    public static File prepareGuestApk(Context c, String id) throws IOException {
        File dir = instanceDir(c, id);
        File apk = new File(dir, "guest.apk");
        // Selalu salin ulang: host bisa di-update dengan guest.apk baru.
        if (apk.exists()) //noinspection ResultOfMethodCallIgnored
            apk.setWritable(true);
        {
            AssetManager am = c.getAssets();
            InputStream in = am.open(ASSET_GUEST);
            FileOutputStream out = new FileOutputStream(apk);
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            out.close();
            in.close();
        }
        // wajib untuk Android 14+ (dynamic code loading)
        //noinspection ResultOfMethodCallIgnored
        apk.setReadOnly();
        return apk;
    }

    public static String fingerprint(JSONObject o) {
        String brand = o.optString("brand", "Google").toLowerCase(Locale.US);
        String code = o.optString("code", "generic");
        String ver = o.optString("android", "16");
        String serial = o.optString("serial", "0000000000");
        String build = "16".equals(ver) ? "BP2A.250705.008" : "AP4A.250305.005";
        return brand + "/" + code + "/" + code + ":" + ver + "/" + build + "/" + serial + ":user/release-keys";
    }

    public static String newImei(String brand) {
        // Prefix TAC yang lazim: 86 untuk Xiaomi/BBK, 35 untuk sisanya.
        String prefix = ("Xiaomi".equals(brand) || "OPPO".equals(brand)
                || "vivo".equals(brand) || "realme".equals(brand)) ? "86" : "35";
        StringBuilder sb = new StringBuilder(prefix);
        // TAC 8 digit + 6 digit acak
        while (sb.length() < 14) sb.append(RNG.nextInt(10));
        String body = sb.toString();
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            int d = body.charAt(body.length() - 1 - i) - '0';
            if (i % 2 == 0) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
        }
        int check = (10 - (sum % 10)) % 10;
        return body + check;
    }

    public static String newMac() {
        int[] b = new int[6];
        for (int i = 0; i < 6; i++) b[i] = RNG.nextInt(256);
        b[0] = (b[0] | 0x02) & 0xFE; // locally administered, unicast
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format(Locale.US, "%02x", b[i]));
        }
        return sb.toString();
    }

    public static String hex(int n) {
        String chars = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(chars.charAt(RNG.nextInt(16)));
        return sb.toString();
    }

    public static String digits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }

    public static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursive(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
