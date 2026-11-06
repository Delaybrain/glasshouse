package com.ansub.tvhome;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

// Photos for the screensaver, sent from a phone or computer in the same Wi-Fi: while the "Photos from
// your phone" card is open, a tiny web server on the TV serves one page to pick photos and takes them
// one by one. They go into Android's media index under Pictures/TV-Fotos, as this app's own: the
// screensaver finds them without the photo permission. It only runs while the card is up.
final class PhotoDrop {
    interface Listener { void received(int count); }

    static final int PORT = 8080;
    private static final String FOLDER = "Pictures/TV-Fotos";
    private static final long MAX_BYTES = 40L << 20; // one photo; a phone's largest are well under this

    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ServerSocket server;
    private volatile int count;

    PhotoDrop(Context c, Listener l) {
        context = c.getApplicationContext();
        listener = l;
    }

    // Starts listening, on PORT if it's free, and returns the port.
    int start() throws IOException {
        server = new ServerSocket();
        server.setReuseAddress(true);
        try {
            server.bind(new java.net.InetSocketAddress(PORT));
        } catch (IOException taken) {
            server.close();
            server = new ServerSocket(0);
        }
        final ServerSocket s = server;
        Thread t = new Thread(() -> {
            while (!s.isClosed()) {
                try { serve(s.accept()); } catch (IOException closed) { return; }
            }
        }, "PhotoDrop");
        t.setDaemon(true);
        t.start();
        return s.getLocalPort();
    }

    void stop() {
        try { if (server != null) server.close(); } catch (IOException ignored) {}
    }

    int count() {
        return count;
    }

    // One request: the page (GET /), or one photo (POST /photo, the file as the body).
    private void serve(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(30_000);
            InputStream in = new BufferedInputStream(s.getInputStream());
            String[] request = line(in).split(" ");
            Map<String, String> headers = new HashMap<>();
            for (String h; !(h = line(in)).isEmpty(); ) {
                int colon = h.indexOf(':');
                if (colon > 0) headers.put(h.substring(0, colon).trim().toLowerCase(), h.substring(colon + 1).trim());
            }
            OutputStream out = s.getOutputStream();
            if (request.length < 2) return;
            if (request[0].equals("GET") && (request[1].equals("/") || request[1].startsWith("/?"))) {
                reply(out, "200 OK", "text/html; charset=utf-8", page());
            } else if (request[0].equals("POST") && request[1].equals("/photo")) {
                long length = parse(headers.get("content-length"));
                String type = headers.get("content-type");
                if (length <= 0 || length > MAX_BYTES || type == null || !type.startsWith("image/")) {
                    reply(out, "413 Payload Too Large", "text/plain", "no");
                    return;
                }
                String name = headers.get("x-name");
                name = name == null ? "photo.jpg" : URLDecoder.decode(name, "UTF-8").replaceAll("[/\\\\:*?\"<>|]", "_");
                if (save(in, length, name, type)) {
                    final int n = ++count;
                    main.post(() -> listener.received(n));
                    reply(out, "200 OK", "text/plain", "ok");
                } else {
                    reply(out, "500 Internal Server Error", "text/plain", "no");
                }
            } else {
                reply(out, "404 Not Found", "text/plain", "no");
            }
        } catch (IOException ignored) {}
    }

    // Streams the body into a new picture in the media index; half-written ones are taken out again.
    private boolean save(InputStream in, long length, String name, String type) {
        ContentResolver cr = context.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, type);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER);
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        Uri u = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (u == null) return false;
        try (OutputStream out = cr.openOutputStream(u)) {
            byte[] buf = new byte[64 * 1024];
            long left = length;
            while (left > 0) {
                int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                if (n < 0) throw new IOException("cut off");
                out.write(buf, 0, n);
                left -= n;
            }
        } catch (Exception e) {
            cr.delete(u, null, null);
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            v.clear();
            v.put(MediaStore.MediaColumns.IS_PENDING, 0);
            cr.update(u, v, null, null);
        }
        return true;
    }

    // Takes out every photo sent this way (only this app's own can be taken out); returns how many.
    static int deleteReceived(Context c) {
        ContentResolver cr = c.getContentResolver();
        Uri all = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        int n = 0;
        try (Cursor cur = cr.query(all, new String[] {MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?", new String[] {FOLDER + "%"}, null)) {
            while (cur != null && cur.moveToNext())
                n += cr.delete(android.content.ContentUris.withAppendedId(all, cur.getLong(0)), null, null);
        } catch (Exception ignored) {}
        return n;
    }

    // The page on the phone, in the TV's language and the home screen's look: a glass card, one white button.
    private static String page() {
        String title = html(Strings.t("Photos from your phone (Wi-Fi)"));
        String choose = html(Strings.t("Choose photos"));
        String sent = Strings.t("Sent").replace("\\", "").replace("'", "\\'");
        return "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'><title>" + title + "</title><style>"
                + "body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
                + "background:linear-gradient(135deg,#3a2a7a,#151826);font:17px -apple-system,Roboto,sans-serif;color:#fff}"
                + ".card{width:min(88vw,420px);padding:28px;border-radius:28px;background:rgba(255,255,255,.12);"
                + "border:1px solid rgba(255,255,255,.28);backdrop-filter:blur(20px);-webkit-backdrop-filter:blur(20px);"
                + "box-shadow:inset 0 1px 0 rgba(255,255,255,.4)}"
                + "h1{font-size:22px;margin:0 0 20px}"
                + "label{display:block;padding:16px;border-radius:16px;background:#fff;color:#111318;text-align:center;"
                + "font-weight:600;cursor:pointer}input{display:none}#s{margin-top:16px;color:rgba(255,255,255,.75);min-height:1.4em}"
                + "</style></head><body><div class='card'><h1>" + title + "</h1>"
                + "<label>" + choose + "<input id='f' type='file' accept='image/*' multiple></label><div id='s'></div></div>"
                + "<script>const f=document.getElementById('f'),s=document.getElementById('s');"
                + "f.onchange=async()=>{const all=[...f.files];let ok=0;"
                + "for(const x of all){s.textContent='" + sent + " '+ok+' / '+all.length;"
                + "try{const r=await fetch('/photo',{method:'POST',headers:{'X-Name':encodeURIComponent(x.name),"
                + "'Content-Type':x.type||'image/jpeg'},body:x});if(r.ok)ok++;}catch(e){}}"
                + "s.textContent='" + sent + " '+ok+' / '+all.length+' \\u2713';f.value='';};</script></body></html>";
    }

    private static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }

    private static void reply(OutputStream out, String status, String type, String body) throws IOException {
        byte[] b = body.getBytes("UTF-8");
        out.write(("HTTP/1.1 " + status + "\r\nContent-Type: " + type + "\r\nContent-Length: " + b.length
                + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        out.write(b);
        out.flush();
    }

    // One line of the request, without its line break.
    private static String line(InputStream in) throws IOException {
        StringBuilder s = new StringBuilder();
        for (int c; (c = in.read()) != -1 && c != '\n'; ) if (c != '\r') s.append((char) c);
        return s.toString();
    }

    private static long parse(String n) {
        try { return Long.parseLong(n); } catch (Exception e) { return -1; }
    }
}
