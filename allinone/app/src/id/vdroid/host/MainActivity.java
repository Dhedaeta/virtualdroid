package id.vdroid.host;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import java.util.Locale;

/**
 * Host shell Jalur A (vertical slice).
 *
 * Yang nyata di build ini:
 *  - instance + identitas per instance
 *  - APK tamu (assets/guest.apk) dimuat lewat DexClassLoader ke classloader terpisah
 *  - data tamu di direktori instance masing-masing
 *  - UI tamu benar-benar berjalan di dalam host
 *
 * Yang belum: hijacking Activity APK arbitrer, hook Build/telephony per proses,
 * dan root kernel. Itu milestone v0.5+ (lihat docs/JALUR-A.md).
 */
public class MainActivity extends Activity {

    private LinearLayout list;
    private TextView count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());

        // Jalur uji otomatis (AVD/CI): am start ... --ez demo true
        if (getIntent().getBooleanExtra("demo", false)) {
            JSONArray arr = Core.load(this);
            JSONObject o = arr.length() > 0 ? arr.optJSONObject(0) : null;
            if (o == null) o = Core.create(this, Core.PRESETS[0]);
            launch(o.optString("id"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#07090C"));

        // ---- header ----
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        int p = dp(22);
        head.setPadding(p, dp(34), p, dp(10));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("V");
        logo.setTextColor(Color.parseColor("#04231A"));
        logo.setTextSize(18);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(bg(Color.parseColor("#3DDC84"), dp(9)));
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView title = new TextView(this);
        title.setText("  VirtualDroid");
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        brandRow.addView(title);

        TextView ver = new TextView(this);
        ver.setText("  v0.5");
        ver.setTextColor(Color.parseColor("#C4B5FD"));
        ver.setTextSize(10);
        ver.setTypeface(Typeface.DEFAULT_BOLD);
        ver.setBackground(bg(Color.parseColor("#241A3F"), dp(5)));
        ver.setPadding(dp(6), dp(3), dp(6), dp(3));
        brandRow.addView(ver);

        head.addView(brandRow);

        TextView sub = new TextView(this);
        sub.setText("Guest APK berjalan di dalam host · tanpa instalasi");
        sub.setTextColor(Color.parseColor("#8A97A8"));
        sub.setTextSize(11);
        sub.setPadding(0, dp(5), 0, 0);
        head.addView(sub);

        // create button
        TextView add = new TextView(this);
        add.setText("+  Buat HP virtual");
        add.setTextColor(Color.parseColor("#04231A"));
        add.setTextSize(14);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setGravity(Gravity.CENTER);
        add.setBackground(bg(Color.parseColor("#3DDC84"), dp(12)));
        add.setPadding(0, dp(14), 0, dp(14));
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickPreset(); }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(16);
        head.addView(add, alp);
        root.addView(head);

        // list header
        LinearLayout listHead = new LinearLayout(this);
        listHead.setOrientation(LinearLayout.HORIZONTAL);
        listHead.setPadding(p, dp(10), p, dp(6));
        TextView lh = new TextView(this);
        lh.setText("PERANGKAT VIRTUAL");
        lh.setTextColor(Color.parseColor("#6B7788"));
        lh.setTextSize(10);
        lh.setTypeface(Typeface.DEFAULT_BOLD);
        lh.setLetterSpacing(0.12f);
        listHead.addView(lh, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        count = new TextView(this);
        count.setTextColor(Color.parseColor("#8A97A8"));
        count.setTextSize(11);
        listHead.addView(count);
        root.addView(listHead);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(p, dp(4), p, dp(18));
        sv.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView foot = new TextView(this);
        foot.setText("Guest berjalan di proses host dengan classloader & data terpisah.\n" +
                "Root nyata ada di Jalur B (vdctl + proot).");
        foot.setTextColor(Color.parseColor("#4F5B6A"));
        foot.setTextSize(9);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(p, dp(8), p, dp(18));
        root.addView(foot);

        return root;
    }

    private void refresh() {
        if (list == null) return;
        list.removeAllViews();
        JSONArray arr = Core.load(this);
        count.setText(arr.length() + " instance");

        if (arr.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada HP virtual.\nTekan “Buat HP virtual” untuk mulai.");
            empty.setTextColor(Color.parseColor("#5F6B7A"));
            empty.setTextSize(12);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            list.addView(empty);
            return;
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.addView(card(o));
        }
    }

    private View card(final JSONObject o) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(bg(Color.parseColor("#111823"), dp(16)));
        c.setPadding(dp(16), dp(14), dp(16), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(o.optString("name"));
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView hook = new TextView(this);
        hook.setText("HOOK");
        hook.setTextColor(Color.parseColor("#C4B5FD"));
        hook.setTextSize(8);
        hook.setTypeface(Typeface.DEFAULT_BOLD);
        hook.setBackground(bg(Color.parseColor("#241A3F"), dp(4)));
        hook.setPadding(dp(6), dp(2), dp(6), dp(2));
        hook.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launch(o.optString("id"), true); }
        });
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.rightMargin = dp(6);
        top.addView(hook, hlp);

        TextView root = new TextView(this);
        root.setText("ROOT");
        root.setTextColor(Color.parseColor("#FFCF70"));
        root.setTextSize(8);
        root.setTypeface(Typeface.DEFAULT_BOLD);
        root.setBackground(bg(Color.parseColor("#332A18"), dp(4)));
        root.setPadding(dp(6), dp(2), dp(6), dp(2));
        top.addView(root);
        c.addView(top);

        TextView meta = new TextView(this);
        meta.setText(o.optString("model") + " · Android " + o.optString("android")
                + " · " + o.optInt("ram") + " GB RAM · " + o.optInt("storage") + " GB");
        meta.setTextColor(Color.parseColor("#8A97A8"));
        meta.setTextSize(11);
        meta.setPadding(0, dp(4), 0, 0);
        c.addView(meta);

        TextView id = new TextView(this);
        id.setText("IMEI " + o.optString("imei") + "  ·  " + o.optString("id"));
        id.setTextColor(Color.parseColor("#556274"));
        id.setTextSize(9);
        id.setPadding(0, dp(2), 0, 0);
        c.addView(id);

        try {
            File gapk = new File(Core.instanceDir(this, o.optString("id")), "guest.apk");
            String act = GuestManifest.launcher(this, gapk);
            if (act != null) {
                TextView man = new TextView(this);
                man.setText("Manifest: " + act + "  ·  "
                        + GuestManifest.activityCount(this, gapk) + " activity");
                man.setTextColor(Color.parseColor("#4F6B5A"));
                man.setTextSize(8.5f);
                man.setPadding(0, dp(2), 0, 0);
                c.addView(man);
            }
        } catch (Throwable ignored) {}

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(12);

        TextView run = new TextView(this);
        run.setText("▶  Jalankan guest");
        run.setTextColor(Color.parseColor("#8FF0BD"));
        run.setTextSize(11);
        run.setTypeface(Typeface.DEFAULT_BOLD);
        run.setGravity(Gravity.CENTER);
        GradientDrawable rg = bg(Color.parseColor("#12251C"), dp(9));
        rg.setStroke(dp(1), Color.parseColor("#2E6B4B"));
        run.setBackground(rg);
        run.setPadding(dp(12), dp(9), dp(12), dp(9));
        run.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launch(o.optString("id")); }
        });
        actions.addView(run, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView del = new TextView(this);
        del.setText("Hapus");
        del.setTextColor(Color.parseColor("#9AA7B8"));
        del.setTextSize(11);
        del.setGravity(Gravity.CENTER);
        del.setBackground(bg(Color.parseColor("#1A222E"), dp(9)));
        del.setPadding(dp(12), dp(9), dp(12), dp(9));
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmDelete(o); }
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.leftMargin = dp(8);
        actions.addView(del, dlp);

        c.addView(actions, actionsLp);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(10);
        c.setLayoutParams(clp);
        return c;
    }

    private void pickPreset() {
        final String[] labels = new String[Core.PRESETS.length];
        for (int i = 0; i < Core.PRESETS.length; i++) {
            String[] p = Core.PRESETS[i];
            labels[i] = String.format(Locale.US, "%s · %s GB · Android %s",
                    p[1], p[6], p[4]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Pilih perangkat")
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        JSONObject o = Core.create(MainActivity.this, Core.PRESETS[which]);
                        refresh();
                        Toast.makeText(MainActivity.this,
                                o.optString("model") + " dibuat", Toast.LENGTH_SHORT).show();
                        launch(o.optString("id"));
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void confirmDelete(final JSONObject o) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus " + o.optString("name") + "?")
                .setMessage("Guest, data, dan identitas instance ini ikut dihapus.")
                .setPositiveButton("Hapus", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        Core.delete(MainActivity.this, o.optString("id"));
                        refresh();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void launch(String id) {
        launch(id, false);
    }

    private void launch(String id, boolean hook) {
        Intent i = new Intent(this, StubActivity.class);
        i.putExtra(StubActivity.EXTRA_ID, id);
        i.putExtra("hook", hook);

        if (hook && VdApp.hookInstalled) {
            i.putExtra("vd_hook", true);
            i.putExtra("vd_instance", id);
            try {
                File apk = new File(Core.instanceDir(this, id), "guest.apk");
                if (!apk.exists()) Core.prepareGuestApk(this, id);
                String act = GuestManifest.launcher(this, apk);
                if (act != null) {
                    i.putExtra("vd_guest_class", act);
                    android.util.Log.i("VirtualDroid", "manifest launcher=" + act);
                }
            } catch (Throwable ignored) {
                // StubActivity tetap jadi fallback
            }
        }
        startActivity(i);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }
}
