package com.delaybrain.glasshouse;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

// The TV's side of "from your phone (Wi-Fi)": while one of those cards is up, a tiny web server on the TV
// serves one page to a phone or computer in the same network. Two kinds:
// - photos: picked (or dropped onto the page) and sent one by one into Android's media index
//   (Pictures/TV-Fotos for the screensaver, TV-Hintergrund for the wallpaper), as this app's own, so the
//   TV shows them without the photo permission;
// - the backup: downloaded as a file, or a file sent back to restore it (see Backup).
// It only runs while the card is up, and takes nothing but pictures, or a backup.
final class PhotoDrop {
    interface Listener { void received(int count, Uri photo); }

    static final int PORT = 8080;
    static final String SCREENSAVER = "Pictures/TV-Fotos", WALLPAPER = "Pictures/TV-Hintergrund"; // where they go
    private static final long MAX_BYTES = 40L << 20, MAX_BACKUP = 256 << 10;

    private final Context context;
    private final String folder; // photos go here; null: the backup page
    private final String title;
    private final Listener listener;
    private final Runnable restored;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ServerSocket server;
    private volatile int count;

    // Photos into folder.
    PhotoDrop(Context c, String folder, String title, Listener l) {
        context = c.getApplicationContext();
        this.folder = folder;
        this.title = title;
        listener = l;
        restored = null;
    }

    // The backup: restored runs (on the main thread) once one sent back is in place.
    PhotoDrop(Context c, String title, Runnable restored) {
        context = c.getApplicationContext();
        folder = null;
        this.title = title;
        listener = null;
        this.restored = restored;
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

    // One request: the page (GET /), a photo (POST /photo, the file as the body), the backup (GET /backup)
    // or one to restore (POST /restore).
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
            String method = request[0], path = request[1];
            long length = parse(headers.get("content-length"));
            if (method.equals("GET") && (path.equals("/") || path.startsWith("/?"))) {
                reply(out, "200 OK", "text/html; charset=utf-8", "", folder != null ? photoPage() : backupPage());
            } else if (folder != null && method.equals("POST") && path.equals("/photo")) {
                String type = headers.get("content-type");
                if (length <= 0 || length > MAX_BYTES || type == null || !type.startsWith("image/")) {
                    reply(out, "413 Payload Too Large", "text/plain", "", "no");
                    return;
                }
                String name = headers.get("x-name");
                name = name == null ? "photo.jpg" : URLDecoder.decode(name, "UTF-8").replaceAll("[/\\\\:*?\"<>|]", "_");
                final Uri saved = save(in, length, name, type);
                if (saved == null) { reply(out, "500 Internal Server Error", "text/plain", "", "no"); return; }
                final int n = ++count;
                main.post(() -> listener.received(n, saved));
                reply(out, "200 OK", "text/plain", "", "ok");
            } else if (folder == null && method.equals("GET") && path.equals("/backup")) {
                String json;
                try { json = Backup.json(context); } catch (Exception e) { reply(out, "500 Internal Server Error", "text/plain", "", "no"); return; }
                reply(out, "200 OK", "application/json; charset=utf-8",
                        "Content-Disposition: attachment; filename=\"glasshouse-backup.json\"\r\n", json);
            } else if (folder == null && method.equals("POST") && path.equals("/restore")) {
                if (length <= 0 || length > MAX_BACKUP) { reply(out, "413 Payload Too Large", "text/plain", "", "no"); return; }
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                for (long left = length; left > 0; ) {
                    int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                    if (n < 0) break;
                    body.write(buf, 0, n);
                    left -= n;
                }
                if (!Backup.apply(context, body.toString("UTF-8"))) { reply(out, "422 Unprocessable Entity", "text/plain", "", "no"); return; }
                count++;
                reply(out, "200 OK", "text/plain", "", "ok");
                main.post(restored);
            } else {
                reply(out, "404 Not Found", "text/plain", "", "no");
            }
        } catch (IOException ignored) {}
    }

    // Streams the body into a new picture in the media index; half-written ones are taken out again.
    private Uri save(InputStream in, long length, String name, String type) {
        ContentResolver cr = context.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, type);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, folder);
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        Uri u = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (u == null) return null;
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
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            v.clear();
            v.put(MediaStore.MediaColumns.IS_PENDING, 0);
            cr.update(u, v, null, null);
        }
        return u;
    }

    // ---- The page, in the TV's language and the home screen's look: the wallpaper's colours behind a
    // card of glass, white buttons with dark text. One file, no outside requests (it works offline).

    private static final String CSS = "*{box-sizing:border-box}"
            + "body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;color:#fff;"
            + "font:16px/1.45 -apple-system,BlinkMacSystemFont,'SF Pro Text',Roboto,'Segoe UI',sans-serif;"
            + "background:radial-gradient(1100px 700px at 12% 8%,#6b4bd8 0,transparent 60%),"
            + "radial-gradient(900px 650px at 92% 95%,#2b5bd6 0,transparent 58%),#151826;background-attachment:fixed}"
            + ".card{width:min(100%,580px);padding:26px;border-radius:32px;border:1px solid rgba(255,255,255,.28);"
            + "background:linear-gradient(180deg,rgba(255,255,255,.17),rgba(255,255,255,.06) 55%);"
            + "backdrop-filter:blur(30px) saturate(140%);-webkit-backdrop-filter:blur(30px) saturate(140%);"
            + "box-shadow:inset 0 1px 0 rgba(255,255,255,.45),0 24px 70px rgba(0,0,0,.35)}"
            + ".head{display:flex;gap:14px;align-items:center;margin-bottom:22px}"
            + ".tv{flex:none;width:46px;height:46px;border-radius:14px;display:grid;place-items:center;"
            + "background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.28)}"
            + "h1{font-size:21px;margin:0;font-weight:600}.sub{color:rgba(255,255,255,.7);font-size:14px}"
            + ".drop{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;min-height:180px;"
            + "padding:22px;text-align:center;border-radius:24px;border:1.5px dashed rgba(255,255,255,.38);"
            + "background:rgba(255,255,255,.05);cursor:pointer;transition:background .2s,border-color .2s,transform .2s}"
            + ".drop.over{background:rgba(255,255,255,.16);border-color:#fff;transform:scale(1.01)}"
            + ".btn{display:inline-block;padding:13px 24px;border-radius:999px;background:#fff;color:#111318;font-weight:600;"
            + "border:0;font-size:16px;text-decoration:none;cursor:pointer;text-align:center}"
            + ".btn.glass{background:rgba(255,255,255,.14);color:#fff;border:1px solid rgba(255,255,255,.3)}"
            + ".hint{color:rgba(255,255,255,.7);font-size:14px}"
            + ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(88px,1fr));gap:10px;margin-top:18px}"
            + ".t{position:relative;aspect-ratio:1;border-radius:14px;overflow:hidden;background:rgba(255,255,255,.08)}"
            + ".t img{width:100%;height:100%;object-fit:cover;opacity:.5;transition:opacity .3s}.t.done img{opacity:1}"
            + ".t.fail img{opacity:.2}.t .bar{position:absolute;left:8px;right:8px;bottom:8px;height:4px;border-radius:2px;"
            + "background:rgba(255,255,255,.25);overflow:hidden}.t .bar i{display:block;height:100%;width:0;background:#fff}"
            + ".t.done .bar,.t.fail .bar{display:none}.t .ok{position:absolute;top:6px;right:6px;width:22px;height:22px;"
            + "border-radius:50%;background:#fff;color:#111318;display:none;place-items:center;font-size:13px;font-weight:700}"
            + ".t.done .ok{display:grid}.total{margin-top:18px;height:6px;border-radius:3px;background:rgba(255,255,255,.18);"
            + "overflow:hidden;display:none}.total i{display:block;height:100%;width:0;background:#fff;transition:width .2s}"
            + "#s{margin-top:12px;color:rgba(255,255,255,.75);font-size:14px;min-height:1.4em}"
            + ".row{display:grid;gap:12px}input{display:none}";

    private static final String TV_ICON = "<svg width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='#fff' "
            + "stroke-width='1.6' stroke-linecap='round'><rect x='3' y='5' width='18' height='12' rx='2'/>"
            + "<path d='M12 17v3M8 20h8'/></svg>";

    private String head() {
        return "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'><title>" + html(title) + "</title>"
                + "<style>" + CSS + "</style></head><body><div class='card'><div class='head'><div class='tv'>" + TV_ICON
                + "</div><div><h1>" + html(title) + "</h1><div class='sub'>"
                + html(String.format(Strings.t("Connected to %s"), tvName())) + "</div></div></div>";
    }

    // The TV's name as the network and the phone's cast menu show it ("PEAQ Home"), else its model.
    private String tvName() {
        String name = android.provider.Settings.Global.getString(context.getContentResolver(),
                android.provider.Settings.Global.DEVICE_NAME);
        return name == null || name.isEmpty() ? android.os.Build.MODEL : name;
    }

    private String photoPage() {
        return head()
                + "<label class='drop' id='drop'><span class='btn'>" + html(Strings.t("Choose photos")) + "</span>"
                + "<span class='hint'>" + html(Strings.t("Drop photos here or choose them")) + "</span>"
                + "<input id='f' type='file' accept='image/*' multiple></label>"
                + "<div class='total' id='total'><i></i></div><div id='s'></div><div class='grid' id='grid'></div></div>"
                + "<script>const done=" + js(Strings.t("Done: %d of %d on the TV")) + ";"
                + "const $=i=>document.getElementById(i),drop=$('drop'),f=$('f'),grid=$('grid'),s=$('s'),total=$('total'),"
                + "bar=total.firstElementChild;let queue=Promise.resolve(),all=0,ok=0,bytes=0,sent=0;"
                + "['dragenter','dragover'].forEach(e=>drop.addEventListener(e,v=>{v.preventDefault();drop.classList.add('over')}));"
                + "['dragleave','drop'].forEach(e=>drop.addEventListener(e,v=>{v.preventDefault();drop.classList.remove('over')}));"
                + "drop.addEventListener('drop',v=>send([...v.dataTransfer.files]));"
                + "f.onchange=()=>{send([...f.files]);f.value=''};"
                + "function send(files){files=files.filter(x=>x.type.startsWith('image/'));if(!files.length)return;"
                + "total.style.display='block';for(const x of files){all++;bytes+=x.size;const t=document.createElement('div');"
                + "t.className='t';t.innerHTML='<img alt=\"\"><div class=\"bar\"><i></i></div><div class=\"ok\">\\u2713</div>';"
                + "t.querySelector('img').src=URL.createObjectURL(x);grid.appendChild(t);queue=queue.then(()=>up(x,t))}show()}"
                + "function up(x,t){return new Promise(res=>{const r=new XMLHttpRequest();let last=0;r.open('POST','/photo');"
                + "r.setRequestHeader('X-Name',encodeURIComponent(x.name));r.setRequestHeader('Content-Type',x.type||'image/jpeg');"
                + "r.upload.onprogress=e=>{if(!e.lengthComputable)return;t.querySelector('.bar i').style.width=(e.loaded/e.total*100)+'%';"
                + "sent+=e.loaded-last;last=e.loaded;show()};"
                + "r.onloadend=()=>{sent+=x.size-last;if(r.status==200){ok++;t.classList.add('done')}else t.classList.add('fail');show();res()};"
                + "r.send(x)})}"
                + "function show(){bar.style.width=(bytes?sent/bytes*100:0)+'%';s.textContent=done.replace('%d',ok).replace('%d',all)}"
                + "</script></body></html>";
    }

    private String backupPage() {
        return head()
                + "<div class='row'><a class='btn' href='/backup' download='glasshouse-backup.json'>"
                + html(Strings.t("Download backup")) + "</a>"
                + "<label class='btn glass'>" + html(Strings.t("Upload backup"))
                + "<input id='f' type='file' accept='.json,application/json'></label></div><div id='s'></div></div>"
                + "<script>const yes=" + js(Strings.t("Restored")) + ",no=" + js(Strings.t("No backup found")) + ";"
                + "const f=document.getElementById('f'),s=document.getElementById('s');"
                + "f.onchange=async()=>{const x=f.files[0];f.value='';if(!x)return;"
                + "try{const r=await fetch('/restore',{method:'POST',body:await x.text()});s.textContent=r.ok?yes+' \\u2713':no}"
                + "catch(e){s.textContent=no}};</script></body></html>";
    }

    private static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }

    // A JavaScript string literal.
    private static String js(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c").replace("\n", "\\n") + "\"";
    }

    private static void reply(OutputStream out, String status, String type, String extraHeaders, String body) throws IOException {
        byte[] b = body.getBytes("UTF-8");
        out.write(("HTTP/1.1 " + status + "\r\nContent-Type: " + type + "\r\nContent-Length: " + b.length
                + "\r\n" + extraHeaders + "Cache-Control: no-store\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
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
