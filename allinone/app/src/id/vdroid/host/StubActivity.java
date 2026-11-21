package id.vdroid.host;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Stub Activity milik host. Activity ini terdaftar di manifest host,
 * tetapi isinya berasal dari APK tamu: layout XML, drawable, string,
 * dan kode GuestEntry semuanya milik guest.
 */
public class StubActivity extends Activity {

    public static final String EXTRA_ID = "instance_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String id = getIntent().getStringExtra(EXTRA_ID);
        final JSONObject inst = id == null ? null : Core.find(this, id);
        if (inst == null) {
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#07090C"));

        // ---- top bar ----
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(30), dp(14), dp(10));
        bar.setBackgroundColor(Color.parseColor("#0D1320"));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(Color.WHITE);
        back.setTextSize(26);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        bar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(6), 0, 0, 0);

        TextView t = new TextView(this);
        t.setText(inst.optString("name"));
        t.setTextColor(Color.WHITE);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(t);

        TextView s = new TextView(this);
        s.setText(inst.optString("model") + " · Android " + inst.optString("android")
                + " · " + inst.optInt("ram") + " GB");
        s.setTextColor(Color.parseColor("#8A97A8"));
        s.setTextSize(10);
        titles.addView(s);

        bar.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView chip = new TextView(this);
        chip.setText("STUB");
        chip.setTextColor(Color.parseColor("#C4B5FD"));
        chip.setTextSize(8);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Color.parseColor("#241A3F"));
        cg.setCornerRadius(dp(5));
        cg.setStroke(dp(1), Color.parseColor("#4C3A82"));
        chip.setBackground(cg);
        chip.setPadding(dp(7), dp(3), dp(7), dp(3));
        bar.addView(chip);

        root.addView(bar);

        // ---- guest content ----
        FrameLayout container = new FrameLayout(this);
        root.addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        String mode = "STUB";
        try {
            GuestRuntime rt = GuestRuntime.load(this, inst);
            View guest;
            if (getIntent().getBooleanExtra("hook", false)) {
                try {
                    guest = ActivityHijack.attach(this, inst, rt);
                    mode = "HOOK";
                } catch (Throwable he) {
                    Log.e("VirtualDroid", "hook gagal, fallback resource proxy", he);
                    guest = rt.createView(this, inst);
                    Toast.makeText(this, "Hook gagal, pakai resource proxy",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                guest = rt.createView(this, inst);
            }
            container.addView(guest, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            s.append(" · loader " + Integer.toHexString(rt.classLoaderHash()));
        } catch (Throwable e) {
            Log.e("VirtualDroid", "gagal memuat guest", e);
            container.addView(errorView(e));
        }
        chip.setText(mode);

        TextView foot = new TextView(this);
        foot.setText("StubActivity host · resource proxy · data "
                + Core.instanceDir(this, id).getName() + " · host tidak di-root");
        foot.setTextColor(Color.parseColor("#3F4B5C"));
        foot.setTextSize(8);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(dp(8), dp(6), dp(8), dp(12));
        root.addView(foot);

        setContentView(root);
    }

    private View errorView(Throwable e) {
        ScrollView sv = new ScrollView(this);
        TextView err = new TextView(this);
        err.setText("Gagal memuat guest.apk\n\n" + e.getClass().getName()
                + "\n" + e.getMessage()
                + "\n\nKalau pesan menyebut hidden API / addAssetPath, perangkat\n"
                + "memblokir resource proxy. Kirim logcat ke pengembang.");
        err.setTextColor(Color.parseColor("#FF8B95"));
        err.setTextSize(11);
        err.setPadding(dp(18), dp(18), dp(18), dp(18));
        sv.addView(err);
        return sv;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
