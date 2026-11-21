package id.vdroid.cloud;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Client cloud phone: menyiapkan container Android 16 di server,
 * lalu membuka layar Android asli lewat ws-scrcpy di WebView.
 */
public class MainActivity extends Activity {

    private EditText server, user, pass;
    private TextView status, dot, provisionBtn, openBtn;
    private Handler handler;
    private boolean busy;
    private int pollCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#07090C"));
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(34), dp(22), dp(28));
        sv.addView(root);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("V");
        logo.setTextColor(Color.parseColor("#04231A"));
        logo.setTextSize(18);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(bg(Color.parseColor("#3DDC84"), dp(9), null));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView title = new TextView(this);
        title.setText("  VirtualDroid Cloud");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        brand.addView(title);
        root.addView(brand);

        TextView sub = new TextView(this);
        sub.setText("Android 16 asli dari container server · stream ke HP");
        sub.setTextColor(Color.parseColor("#8A97A8"));
        sub.setTextSize(11);
        sub.setPadding(0, dp(5), 0, dp(18));
        root.addView(sub);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(bg(Color.parseColor("#0F1520"), dp(16), "#1C2634"));
        card.setPadding(dp(15), dp(14), dp(15), dp(14));

        TextView label = new TextView(this);
        label.setText("SERVER");
        label.setTextColor(Color.parseColor("#6B7788"));
        label.setTextSize(8.5f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setLetterSpacing(0.12f);
        card.addView(label);

        server = field("http://IP-VPS", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI);
        card.addView(server, margin(dp(8), 0));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        user = field("admin", InputType.TYPE_CLASS_TEXT);
        pass = field("password", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        row.addView(user, weight(dp(1), dp(4)));
        row.addView(pass, weight(dp(1), dp(8)));
        card.addView(row, margin(dp(8), 0));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView check = smallBtn("Cek server", false);
        check.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { checkServer(); }
        });
        actions.addView(check, weight(1, dp(4)));

        provisionBtn = smallBtn("Siapkan Android 16", true);
        provisionBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { provision(); }
        });
        actions.addView(provisionBtn, weight(1, dp(8)));
        card.addView(actions, margin(dp(12), 0));

        root.addView(card);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.HORIZONTAL);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setBackground(bg(Color.parseColor("#0A0F16"), dp(12), "#141C27"));
        statusCard.setPadding(dp(13), dp(11), dp(13), dp(11));

        dot = new TextView(this);
        dot.setBackground(bg(Color.parseColor("#3A4656"), dp(5), null));
        statusCard.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));

        status = new TextView(this);
        status.setText("Belum terhubung");
        status.setTextColor(Color.parseColor("#8A97A8"));
        status.setTextSize(11);
        status.setPadding(dp(9), 0, 0, 0);
        statusCard.addView(status);
        root.addView(statusCard, margin(dp(10), 0));

        openBtn = new TextView(this);
        openBtn.setText("Buka Layar Android");
        openBtn.setTextColor(Color.parseColor("#04231A"));
        openBtn.setTextSize(14);
        openBtn.setTypeface(Typeface.DEFAULT_BOLD);
        openBtn.setGravity(Gravity.CENTER);
        openBtn.setBackground(bg(Color.parseColor("#3DDC84"), dp(12), null));
        openBtn.setPadding(0, dp(15), 0, dp(15));
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScreen(); }
        });
        root.addView(openBtn, margin(dp(14), 0));

        TextView hint = new TextView(this);
        hint.setText("Tanpa GApps. Root tersedia di container. Play Integrity tidak lulus.\n"
                + "Butuh Oracle Always Free (ARM64) atau VPS Linux dengan binder.");
        hint.setTextColor(Color.parseColor("#4F5B6A"));
        hint.setTextSize(9);
        hint.setGravity(Gravity.CENTER);
        hint.setLineSpacing(dp(2), 1f);
        hint.setPadding(dp(4), dp(16), dp(4), 0);
        root.addView(hint);

        String s = Prefs.server(this);
        if (!s.isEmpty()) {
            server.setText(s);
            user.setText(Prefs.user(this));
            pass.setText(Prefs.pass(this));
            setStatus("Server tersimpan. Tekan Cek server.", "#8A97A8");
        }
        return sv;
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.parseColor("#E9EEF6"));
        e.setHintTextColor(Color.parseColor("#4F5B6A"));
        e.setTextSize(11.5f);
        e.setInputType(type);
        e.setSingleLine(true);
        e.setBackground(bg(Color.parseColor("#0A0E15"), dp(10), "#1C2634"));
        e.setPadding(dp(11), dp(9), dp(11), dp(9));
        return e;
    }

    private TextView smallBtn(String text, boolean primary) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(10.5f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, dp(10), 0, dp(10));
        if (primary) {
            b.setTextColor(Color.parseColor("#04231A"));
            b.setBackground(bg(Color.parseColor("#3DDC84"), dp(9), null));
        } else {
            b.setTextColor(Color.parseColor("#C9D6E4"));
            b.setBackground(bg(Color.parseColor("#161F2C"), dp(9), "#263243"));
        }
        return b;
    }

    private void save() {
        Prefs.save(this, Api.normalize(server.getText().toString()),
                user.getText().toString().trim(),
                pass.getText().toString());
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    private void checkServer() {
        if (busy) return;
        save();
        final String url = Api.normalize(server.getText().toString());
        final String u = user.getText().toString().trim();
        final String p = pass.getText().toString();
        if (url.isEmpty()) { toast("Isi server dulu"); return; }

        setBusy(true);
        setStatus("Menghubungi server...", "#FFC862");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final JSONObject o = Api.get(url, "/api/status", u, p);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            applyStatus(o);
                            setBusy(false);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setStatus("Gagal: " + e.getMessage(), "#FF8B95");
                            setBusy(false);
                        }
                    });
                }
            }
        }).start();
    }

    private void provision() {
        if (busy) return;
        save();
        final String url = Api.normalize(server.getText().toString());
        final String u = user.getText().toString().trim();
        final String p = pass.getText().toString();
        if (url.isEmpty()) { toast("Isi server dulu"); return; }

        setBusy(true);
        pollCount = 0;
        setStatus("Meminta server menyiapkan Android 16...", "#FFC862");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Api.post(url, "/api/provision", u, p);
                    runOnUiThread(new Runnable() {
                        @Override public void run() { poll(url, u, p); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setStatus("Gagal: " + e.getMessage(), "#FF8B95");
                            setBusy(false);
                        }
                    });
                }
            }
        }).start();
    }

    private void poll(final String url, final String u, final String p) {
        if (pollCount++ > 100) {
            setStatus("Timeout menunggu Android.", "#FF8B95");
            setBusy(false);
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final JSONObject o = Api.get(url, "/api/status", u, p);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            String st = o.optString("status");
                            applyStatus(o);
                            if ("ready".equals(st) || "error".equals(st)) {
                                setBusy(false);
                            } else {
                                handler.postDelayed(new Runnable() {
                                    @Override public void run() { poll(url, u, p); }
                                }, 3000);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            handler.postDelayed(new Runnable() {
                                @Override public void run() { poll(url, u, p); }
                            }, 4000);
                        }
                    });
                }
            }
        }).start();
    }

    private void applyStatus(JSONObject o) {
        String st = o.optString("status", "idle");
        String msg = o.optString("message", "");
        if ("ready".equals(st)) setStatus("Android siap · " + msg, "#6EE7A8");
        else if ("error".equals(st)) setStatus("Error: " + msg, "#FF8B95");
        else if ("pulling".equals(st) || "starting".equals(st)) setStatus(msg, "#FFC862");
        else setStatus(msg.isEmpty() ? "Server terhubung" : msg, "#8A97A8");
    }

    private void setStatus(String text, String color) {
        status.setText(text);
        status.setTextColor(Color.parseColor(color));
        dot.setBackground(bg(Color.parseColor(color), dp(5), null));
    }

    private void setBusy(boolean b) {
        busy = b;
        provisionBtn.setAlpha(b ? 0.5f : 1f);
        openBtn.setAlpha(b ? 0.6f : 1f);
    }

    private void openScreen() {
        save();
        String url = Api.normalize(server.getText().toString());
        if (url.isEmpty()) { toast("Isi server dulu"); return; }
        Intent i = new Intent(this, CloudActivity.class);
        i.putExtra("server", url);
        i.putExtra("user", user.getText().toString().trim());
        i.putExtra("pass", pass.getText().toString());
        startActivity(i);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private LinearLayout.LayoutParams margin(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private LinearLayout.LayoutParams weight(int weight, int rightMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        p.rightMargin = rightMargin;
        return p;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable bg(int color, int radius, String stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (stroke != null) g.setStroke(dp(1), Color.parseColor(stroke));
        return g;
    }
}
