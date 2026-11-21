package id.vdroid.cloud;

import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class Api {

    private Api() {}

    static String normalize(String server) {
        String s = server.trim();
        if (s.isEmpty()) return "";
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://" + s;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    static JSONObject get(String server, String path, String user, String pass) throws Exception {
        return request("GET", server, path, user, pass);
    }

    static JSONObject post(String server, String path, String user, String pass) throws Exception {
        return request("POST", server, path, user, pass);
    }

    private static JSONObject request(String method, String server, String path,
                                      String user, String pass) throws Exception {
        String base = normalize(server);
        if (base.isEmpty()) throw new IOException("Server belum diisi");

        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setRequestProperty("Accept", "application/json");
        if (user != null && !user.isEmpty()) {
            String token = Base64.encodeToString((user + ":" + pass).getBytes("UTF-8"),
                    Base64.NO_WRAP);
            c.setRequestProperty("Authorization", "Basic " + token);
        }
        if ("POST".equals(method)) {
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(0);
            OutputStream os = c.getOutputStream();
            os.close();
        }

        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String body = read(in);
        c.disconnect();

        if (code >= 400) throw new IOException("HTTP " + code + ": " + trim(body));
        return new JSONObject(body);
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
