package com.ansub.tvhome;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// The home screen's own labels, in the TV's language. App names, HDMI names and the clock come
// from the TV already. A language without a table, or a label without a translation, stays in
// English. To add a language, add a table below in the same order as EN.
final class Strings {
    private static final String[] EN = {
        "Settings", "Add app", "Network", "Offline", "Sound", "Picture",
        "Move", "Replace", "Remove from home", "Uninstall", "App info",
        "Use the arrows to move it, then press OK", "Every app is already here", "Add an app", "Replace %s", "Wallpaper", "TV settings", "Brightness",
        "HDMI input", "All apps", "Live TV", "Control Center", "All settings",
        "Last opened", "just now", "No device on", "Sleep after", "Off", "min", "TV off in", "The TV turns off in a minute", "is on, press OK to watch", "Picture found", "No signal", "Screensaver after", "Screensaver", "Arrange apps", "Pick an app with OK, move it, OK puts it down. Back: done",
        "Tiles", "Original", "Dark", "Light",
        "Glass", "Black & white", "Spotlight", "Status bar", "Clock", "Date", "Download speed", "Off timer",
        "CPU clock", "RAM usage", "RAM in GB", "Brand colours too",
        "Light by day, dark at night", "Recently used",
        "Kids lock", "Choose apps", "Change PIN", "Turn off kids lock", "Enter PIN", "New PIN", "Repeat the PIN",
        "Wrong PIN", "The PINs don't match", "Back up & restore", "Back up now", "Restore", "Backed up", "Restored",
        "No backup found", "Colour gradients", "Your photos (USB)", "No photos found",
        "Photos from your phone (Wi-Fi)", "On your phone or computer, in the same Wi-Fi, open:",
        "Photos received: %d", "Choose photos", "Sent", "Back: done", "Show now", "Delete received photos",
        "Photos deleted: %d",
    };

    // Keyed by language ("de"), or language-country ("zh-TW") where a country writes it differently.
    private static final Map<String, String[]> LANGS = new HashMap<>();
    static {
        LANGS.put("ar", new String[] {
            "الإعدادات", "إضافة تطبيق", "الشبكة", "غير متصل", "الصوت", "الصورة",
            "نقل", "استبدال", "إزالة من الشاشة الرئيسية", "إلغاء التثبيت", "معلومات التطبيق",
            "استخدم الأسهم لنقله، ثم اضغط OK", "كل التطبيقات موجودة هنا", "إضافة تطبيق", "استبدال %s", "الخلفية", "إعدادات التلفزيون", "السطوع",
            "مدخل HDMI", "كل التطبيقات", "البث المباشر", "مركز التحكم", "كل الإعدادات", "آخر فتح", "الآن",
            "لا يوجد جهاز قيد التشغيل", "السكون بعد", "إيقاف", "د", "إيقاف التلفزيون خلال",
            "سيتوقف التلفزيون خلال دقيقة", "قيد التشغيل، اضغط OK للمشاهدة", "توجد صورة", "لا توجد إشارة",
            "شاشة التوقف بعد", "شاشة التوقف", "ترتيب التطبيقات", "اختر تطبيقًا بـ OK، انقله، ثم OK لوضعه. رجوع: تم",
            "المربعات", "الأصلي", "داكن", "فاتح", "زجاج", "أبيض وأسود", "تسليط الضوء", "شريط الحالة", "الساعة",
            "التاريخ", "سرعة التنزيل", "مؤقت الإيقاف", "تردد المعالج", "استخدام الذاكرة", "الذاكرة بالجيجابايت",
            "ألوان العلامات أيضًا", "فاتح نهارًا، داكن ليلًا", "المستخدمة مؤخرًا",
            "قفل الأطفال", "اختيار التطبيقات", "تغيير الرمز", "إيقاف قفل الأطفال", "أدخل الرمز", "رمز جديد",
            "أعد إدخال الرمز", "رمز خاطئ", "الرمزان غير متطابقين", "النسخ الاحتياطي والاستعادة", "نسخ احتياطي الآن",
            "استعادة", "تم النسخ الاحتياطي", "تمت الاستعادة", "لا توجد نسخة احتياطية", "تدرجات لونية", "صورك (USB)",
            "لا توجد صور",
            "صور من هاتفك (Wi-Fi)", "افتح على هاتفك أو حاسوبك في شبكة Wi-Fi نفسها:", "الصور المستلمة: %d",
            "اختر الصور", "تم الإرسال", "رجوع: تم", "عرض الآن", "حذف الصور المستلمة", "الصور المحذوفة: %d",
        });
        LANGS.put("de", new String[] {
            "Einstellungen", "App hinzufügen", "Netzwerk", "Offline", "Ton", "Bild",
            "Verschieben", "Ersetzen", "Vom Startbildschirm entfernen", "Deinstallieren", "App-Info",
            "Mit den Pfeiltasten verschieben, dann OK drücken", "Alle Apps sind schon da", "App hinzufügen", "%s ersetzen", "Hintergrund", "TV-Einstellungen", "Helligkeit",
            "HDMI-Eingang", "Alle Apps", "Live-TV", "Kontrollzentrum", "Alle Einstellungen",
            "Zuletzt geöffnet", "gerade eben", "kein Gerät eingeschaltet", "Ruhezustand nach", "Aus", "Min.", "TV aus in", "Der TV geht in einer Minute aus", "ist an  ·  OK zum Umschalten", "Bild erkannt", "Kein Signal", "Bildschirmschoner nach", "Bildschirmschoner", "Apps anordnen", "Mit OK eine App wählen, mit den Pfeilen verschieben, OK legt sie ab. Zurück: fertig",
            "Kacheln", "Original", "Dunkel", "Hell",
            "Glas", "Schwarzweiß", "Spotlight", "Statusleiste", "Uhr", "Datum", "Download-Rate", "Ausschalt-Timer",
            "CPU-Takt", "RAM-Auslastung", "RAM in GB", "Markenfarben mitfärben",
            "Tags hell, abends dunkel", "Zuletzt verwendet",
            "Kindersperre", "Apps auswählen", "PIN ändern", "Kindersperre aus", "PIN eingeben", "Neue PIN",
            "PIN wiederholen", "Falsche PIN", "Die PINs stimmen nicht überein", "Sichern & wiederherstellen",
            "Jetzt sichern", "Wiederherstellen", "Gesichert", "Wiederhergestellt", "Keine Sicherung gefunden",
            "Farbverläufe", "Eigene Fotos (USB)", "Keine Fotos gefunden",
            "Fotos vom Handy (WLAN)", "Auf dem Handy oder PC im selben WLAN öffnen:", "Empfangene Fotos: %d",
            "Fotos auswählen", "Gesendet", "Zurück: fertig", "Jetzt ansehen", "Empfangene Fotos löschen",
            "Fotos gelöscht: %d",
        });
        LANGS.put("es", new String[] {
            "Ajustes", "Añadir app", "Red", "Sin conexión", "Sonido", "Imagen",
            "Mover", "Reemplazar", "Quitar del inicio", "Desinstalar", "Info. de la app",
            "Usa las flechas para moverla y pulsa OK", "Todas las apps ya están aquí", "Añadir una app", "Reemplazar %s", "Fondo de pantalla", "Ajustes de la TV", "Brillo",
            "Entrada HDMI", "Todas las apps", "TV en directo", "Centro de control", "Todos los ajustes",
            "Abierta por última vez", "ahora mismo", "Ningún dispositivo encendido", "Suspender tras", "Desactivado",
            "min", "La TV se apaga en", "La TV se apaga en un minuto", "está encendido, pulsa OK para verlo",
            "Hay imagen", "Sin señal", "Salvapantallas tras", "Salvapantallas", "Ordenar apps",
            "Elige una app con OK, muévela, OK la suelta. Atrás: listo", "Mosaicos", "Original", "Oscuro", "Claro",
            "Cristal", "Blanco y negro", "Foco", "Barra de estado", "Reloj", "Fecha", "Velocidad de descarga",
            "Temporizador de apagado", "Frecuencia de CPU", "Uso de RAM", "RAM en GB", "Colores de marca también",
            "Claro de día, oscuro de noche", "Usadas recientemente",
            "Control parental", "Elegir apps", "Cambiar PIN", "Desactivar control parental", "Introduce el PIN",
            "PIN nuevo", "Repite el PIN", "PIN incorrecto", "Los PIN no coinciden", "Copia y restauración",
            "Hacer copia ahora", "Restaurar", "Copia guardada", "Restaurado", "No hay ninguna copia",
            "Degradados de color", "Tus fotos (USB)", "No hay fotos",
            "Fotos desde el móvil (Wi-Fi)", "En el móvil o el ordenador, en la misma Wi-Fi, abre:",
            "Fotos recibidas: %d", "Elegir fotos", "Enviadas", "Atrás: listo", "Ver ahora", "Borrar fotos recibidas",
            "Fotos borradas: %d",
        });
        LANGS.put("fr", new String[] {
            "Paramètres", "Ajouter une app", "Réseau", "Hors ligne", "Son", "Image",
            "Déplacer", "Remplacer", "Retirer de l'accueil", "Désinstaller", "Infos sur l'app",
            "Utilisez les flèches pour la déplacer, puis appuyez sur OK", "Toutes les apps sont déjà là", "Ajouter une app", "Remplacer %s", "Fond d'écran", "Paramètres du téléviseur", "Luminosité",
            "Entrée HDMI", "Toutes les apps", "TV en direct", "Centre de contrôle", "Tous les paramètres",
            "Dernière ouverture", "à l'instant", "Aucun appareil allumé", "Veille après", "Désactivé", "min",
            "Arrêt de la TV dans", "La TV s'éteint dans une minute", "est allumé, appuyez sur OK pour regarder",
            "Image détectée", "Aucun signal", "Économiseur d'écran après", "Économiseur d'écran",
            "Organiser les apps", "Choisissez une app avec OK, déplacez-la, OK la pose. Retour : terminé",
            "Vignettes", "Original", "Sombre", "Clair", "Verre", "Noir et blanc", "Projecteur", "Barre d'état",
            "Horloge", "Date", "Débit de téléchargement", "Minuterie d'arrêt", "Fréquence CPU",
            "Utilisation de la RAM", "RAM en Go", "Couleurs de marque aussi", "Clair le jour, sombre le soir",
            "Utilisées récemment",
            "Contrôle parental", "Choisir les apps", "Changer le code", "Désactiver le contrôle parental",
            "Saisissez le code", "Nouveau code", "Répétez le code", "Code incorrect",
            "Les codes ne correspondent pas", "Sauvegarde et restauration", "Sauvegarder", "Restaurer", "Sauvegardé",
            "Restauré", "Aucune sauvegarde trouvée", "Dégradés de couleurs", "Vos photos (USB)",
            "Aucune photo trouvée",
            "Photos depuis le téléphone (Wi-Fi)", "Sur le téléphone ou l'ordinateur, sur le même Wi-Fi, ouvrez :",
            "Photos reçues : %d", "Choisir des photos", "Envoyées", "Retour : terminé", "Voir maintenant",
            "Supprimer les photos reçues", "Photos supprimées : %d",
        });
        LANGS.put("hi", new String[] {
            "सेटिंग्स", "ऐप जोड़ें", "नेटवर्क", "ऑफ़लाइन", "ध्वनि", "चित्र",
            "ले जाएँ", "बदलें", "होम से हटाएँ", "अनइंस्टॉल करें", "ऐप की जानकारी",
            "तीर से खिसकाएँ, फिर OK दबाएँ", "सभी ऐप पहले से यहाँ हैं", "ऐप जोड़ें", "%s बदलें", "वॉलपेपर", "टीवी सेटिंग्स", "चमक",
            "HDMI इनपुट", "सभी ऐप", "लाइव टीवी", "कंट्रोल सेंटर", "सभी सेटिंग्स", "पिछली बार खोला", "अभी",
            "कोई डिवाइस चालू नहीं", "इसके बाद स्लीप", "बंद", "मिनट", "टीवी बंद होगा",
            "टीवी एक मिनट में बंद हो जाएगा", "चालू है, देखने के लिए OK दबाएँ", "तस्वीर मिल रही है",
            "कोई सिग्नल नहीं", "इसके बाद स्क्रीनसेवर", "स्क्रीनसेवर", "ऐप व्यवस्थित करें",
            "OK से ऐप चुनें, खिसकाएँ, OK से रखें। वापस: हो गया", "टाइलें", "मूल", "गहरा", "हल्का", "ग्लास",
            "श्वेत-श्याम", "स्पॉटलाइट", "स्टेटस बार", "घड़ी", "तारीख", "डाउनलोड गति", "बंद होने का टाइमर",
            "CPU क्लॉक", "RAM उपयोग", "RAM GB में", "ब्रांड रंग भी", "दिन में हल्का, रात में गहरा",
            "हाल ही में इस्तेमाल",
            "चाइल्ड लॉक", "ऐप चुनें", "PIN बदलें", "चाइल्ड लॉक बंद करें", "PIN डालें", "नया PIN", "PIN दोबारा डालें",
            "गलत PIN", "PIN मेल नहीं खाते", "बैकअप और रीस्टोर", "अभी बैकअप लें", "रीस्टोर करें", "बैकअप हो गया",
            "रीस्टोर हो गया", "कोई बैकअप नहीं मिला", "रंगों के ग्रेडिएंट", "आपकी फ़ोटो (USB)", "कोई फ़ोटो नहीं मिली",
            "फ़ोन से फ़ोटो (Wi-Fi)", "उसी Wi-Fi पर अपने फ़ोन या कंप्यूटर में खोलें:", "मिली फ़ोटो: %d",
            "फ़ोटो चुनें", "भेजी गईं", "वापस: हो गया", "अभी देखें", "मिली फ़ोटो हटाएँ", "हटाई गई फ़ोटो: %d",
        });
        String[] id = {
            "Setelan", "Tambah aplikasi", "Jaringan", "Offline", "Suara", "Gambar",
            "Pindahkan", "Ganti", "Hapus dari beranda", "Uninstal", "Info aplikasi",
            "Gunakan tombol panah untuk memindahkan, lalu tekan OK", "Semua aplikasi sudah ada di sini", "Tambah aplikasi", "Ganti %s", "Wallpaper", "Setelan TV", "Kecerahan",
            "Input HDMI", "Semua aplikasi", "TV Live", "Pusat Kontrol", "Semua setelan", "Terakhir dibuka",
            "baru saja", "Tidak ada perangkat yang menyala", "Tidur setelah", "Mati", "mnt", "TV mati dalam",
            "TV akan mati dalam satu menit", "menyala, tekan OK untuk menonton", "Ada gambar", "Tidak ada sinyal",
            "Screensaver setelah", "Screensaver", "Atur aplikasi",
            "Pilih aplikasi dengan OK, pindahkan, OK untuk meletakkan. Kembali: selesai", "Ubin", "Asli", "Gelap",
            "Terang", "Kaca", "Hitam putih", "Sorotan", "Bilah status", "Jam", "Tanggal", "Kecepatan unduh",
            "Timer mati", "Kecepatan CPU", "Pemakaian RAM", "RAM dalam GB", "Warna merek juga",
            "Terang siang, gelap malam", "Baru dipakai",
            "Kunci anak", "Pilih aplikasi", "Ubah PIN", "Matikan kunci anak", "Masukkan PIN", "PIN baru",
            "Ulangi PIN", "PIN salah", "PIN tidak cocok", "Cadangkan & pulihkan", "Cadangkan sekarang", "Pulihkan",
            "Tercadangkan", "Dipulihkan", "Cadangan tidak ditemukan", "Gradasi warna", "Foto Anda (USB)",
            "Foto tidak ditemukan",
            "Foto dari ponsel (Wi-Fi)", "Di ponsel atau komputer, di Wi-Fi yang sama, buka:", "Foto diterima: %d",
            "Pilih foto", "Terkirim", "Kembali: selesai", "Lihat sekarang", "Hapus foto yang diterima",
            "Foto dihapus: %d",
        };
        LANGS.put("id", id);
        LANGS.put("in", id); // Java's old code for Indonesian, which Android still reports

        LANGS.put("it", new String[] {
            "Impostazioni", "Aggiungi app", "Rete", "Offline", "Audio", "Immagine",
            "Sposta", "Sostituisci", "Rimuovi dalla home", "Disinstalla", "Info app",
            "Usa le frecce per spostarla, poi premi OK", "Tutte le app sono già qui", "Aggiungi un'app", "Sostituisci %s", "Sfondo", "Impostazioni TV", "Luminosità",
            "Ingresso HDMI", "Tutte le app", "TV in diretta", "Centro di controllo", "Tutte le impostazioni",
            "Ultima apertura", "adesso", "Nessun dispositivo acceso", "Sospensione dopo", "Disattivato", "min",
            "TV spenta tra", "La TV si spegne tra un minuto", "è acceso, premi OK per guardare", "Immagine presente",
            "Nessun segnale", "Salvaschermo dopo", "Salvaschermo", "Riordina app",
            "Scegli un'app con OK, spostala, OK la posa. Indietro: fine", "Riquadri", "Originale", "Scuro", "Chiaro",
            "Vetro", "Bianco e nero", "Riflettore", "Barra di stato", "Orologio", "Data", "Velocità di download",
            "Timer di spegnimento", "Frequenza CPU", "Uso della RAM", "RAM in GB", "Anche i colori del marchio",
            "Chiaro di giorno, scuro di sera", "Usate di recente",
            "Blocco bambini", "Scegli le app", "Cambia PIN", "Disattiva blocco bambini", "Inserisci il PIN",
            "Nuovo PIN", "Ripeti il PIN", "PIN errato", "I PIN non coincidono", "Backup e ripristino",
            "Esegui backup ora", "Ripristina", "Backup eseguito", "Ripristinato", "Nessun backup trovato",
            "Sfumature di colore", "Le tue foto (USB)", "Nessuna foto trovata",
            "Foto dal telefono (Wi-Fi)", "Sul telefono o sul computer, nella stessa Wi-Fi, apri:",
            "Foto ricevute: %d", "Scegli le foto", "Inviate", "Indietro: fine", "Mostra ora",
            "Elimina le foto ricevute", "Foto eliminate: %d",
        });
        LANGS.put("ja", new String[] {
            "設定", "アプリを追加", "ネットワーク", "オフライン", "サウンド", "画像",
            "移動", "置き換え", "ホームから削除", "アンインストール", "アプリ情報",
            "矢印キーで移動して、OK を押してください", "すべてのアプリが追加済みです", "アプリを追加", "%s を置き換え", "壁紙", "テレビの設定", "明るさ",
            "HDMI入力", "すべてのアプリ", "ライブTV", "コントロールセンター", "すべての設定", "最後に開いた日時", "たった今", "電源が入っている機器なし", "スリープまで", "オフ",
            "分", "テレビの電源オフまで", "まもなくテレビの電源が切れます", "がオン、OKで視聴", "映像あり", "信号なし", "スクリーンセーバーまで", "スクリーンセーバー",
            "アプリを並べ替え", "OKでアプリを選び、移動して、OKで置きます。戻る: 完了", "タイル", "オリジナル", "ダーク", "ライト", "ガラス", "白黒", "スポットライト",
            "ステータスバー", "時計", "日付", "ダウンロード速度", "オフタイマー", "CPUクロック", "RAM使用率", "RAM (GB)", "ブランドカラーも", "昼はライト、夜はダーク",
            "最近使ったアプリ",
            "キッズロック", "アプリを選択", "PINを変更", "キッズロックをオフ", "PINを入力", "新しいPIN", "PINをもう一度", "PINが違います", "PINが一致しません",
            "バックアップと復元", "今すぐバックアップ", "復元", "バックアップしました", "復元しました", "バックアップが見つかりません", "カラーグラデーション", "あなたの写真 (USB)",
            "写真が見つかりません",
            "スマホから写真 (Wi-Fi)", "同じWi-Fiのスマホやパソコンで開いてください:", "受信した写真: %d", "写真を選ぶ", "送信済み", "戻る: 完了", "今すぐ表示",
            "受信した写真を削除", "削除した写真: %d",
        });
        LANGS.put("ko", new String[] {
            "설정", "앱 추가", "네트워크", "오프라인", "소리", "화면",
            "이동", "바꾸기", "홈에서 삭제", "제거", "앱 정보",
            "화살표로 옮긴 다음 OK를 누르세요", "모든 앱이 이미 있습니다", "앱 추가", "%s 바꾸기", "배경화면", "TV 설정", "밝기",
            "HDMI 입력", "모든 앱", "실시간 TV", "제어 센터", "모든 설정", "마지막으로 열림", "방금", "켜진 기기 없음", "절전 모드까지", "끔", "분",
            "TV 꺼짐까지", "1분 후 TV가 꺼집니다", "켜짐, OK를 눌러 시청", "화면 있음", "신호 없음", "화면 보호기까지", "화면 보호기", "앱 정렬",
            "OK로 앱을 고르고 옮긴 뒤 OK로 놓으세요. 뒤로: 완료", "타일", "원본", "어둡게", "밝게", "유리", "흑백", "스포트라이트", "상태 표시줄", "시계", "날짜",
            "다운로드 속도", "꺼짐 타이머", "CPU 클럭", "RAM 사용량", "RAM (GB)", "브랜드 색상도", "낮에는 밝게, 밤에는 어둡게", "최근 사용",
            "어린이 잠금", "앱 선택", "PIN 변경", "어린이 잠금 끄기", "PIN 입력", "새 PIN", "PIN 다시 입력", "PIN이 틀렸습니다", "PIN이 일치하지 않습니다",
            "백업 및 복원", "지금 백업", "복원", "백업했습니다", "복원했습니다", "백업이 없습니다", "색상 그라데이션", "내 사진 (USB)", "사진이 없습니다",
            "휴대폰에서 사진 보내기 (Wi-Fi)", "같은 Wi-Fi의 휴대폰이나 컴퓨터에서 열기:", "받은 사진: %d", "사진 선택", "보냄", "뒤로: 완료", "지금 보기",
            "받은 사진 삭제", "삭제한 사진: %d",
        });
        LANGS.put("nl", new String[] {
            "Instellingen", "App toevoegen", "Netwerk", "Offline", "Geluid", "Beeld",
            "Verplaatsen", "Vervangen", "Van startscherm halen", "Verwijderen", "App-info",
            "Verplaats met de pijltjes en druk dan op OK", "Alle apps staan er al", "Een app toevoegen", "%s vervangen", "Achtergrond", "Tv-instellingen", "Helderheid",
            "HDMI-ingang", "Alle apps", "Live-tv", "Bedieningspaneel", "Alle instellingen", "Laatst geopend",
            "zojuist", "Geen apparaat aan", "Slaapstand na", "Uit", "min", "Tv uit over",
            "De tv gaat over een minuut uit", "staat aan, druk op OK om te kijken", "Beeld", "Geen signaal",
            "Schermbeveiliging na", "Schermbeveiliging", "Apps ordenen",
            "Kies een app met OK, verplaats hem, OK zet hem neer. Terug: klaar", "Tegels", "Origineel", "Donker",
            "Licht", "Glas", "Zwart-wit", "Spotlight", "Statusbalk", "Klok", "Datum", "Downloadsnelheid",
            "Uitschakeltimer", "CPU-kloksnelheid", "RAM-gebruik", "RAM in GB", "Ook merkkleuren",
            "Overdag licht, 's avonds donker", "Recent gebruikt",
            "Kinderslot", "Apps kiezen", "Pincode wijzigen", "Kinderslot uitzetten", "Voer de pincode in",
            "Nieuwe pincode", "Herhaal de pincode", "Onjuiste pincode", "De pincodes komen niet overeen",
            "Back-up en herstel", "Nu back-up maken", "Herstellen", "Back-up gemaakt", "Hersteld",
            "Geen back-up gevonden", "Kleurverlopen", "Je foto's (USB)", "Geen foto's gevonden",
            "Foto's van je telefoon (wifi)", "Open op je telefoon of computer, op hetzelfde wifi:",
            "Foto's ontvangen: %d", "Foto's kiezen", "Verzonden", "Terug: klaar", "Nu bekijken",
            "Ontvangen foto's verwijderen", "Foto's verwijderd: %d",
        });
        LANGS.put("pl", new String[] {
            "Ustawienia", "Dodaj aplikację", "Sieć", "Offline", "Dźwięk", "Obraz",
            "Przenieś", "Zamień", "Usuń z ekranu głównego", "Odinstaluj", "Informacje o aplikacji",
            "Przesuń strzałkami, potem naciśnij OK", "Wszystkie aplikacje już tu są", "Dodaj aplikację", "Zamień %s", "Tapeta", "Ustawienia telewizora", "Jasność",
            "Wejście HDMI", "Wszystkie aplikacje", "Telewizja na żywo", "Centrum sterowania", "Wszystkie ustawienia",
            "Ostatnio otwarte", "przed chwilą", "Brak włączonego urządzenia", "Uśpienie po", "Wył.", "min",
            "Wyłączenie TV za", "Telewizor wyłączy się za minutę", "jest włączone, naciśnij OK, aby oglądać",
            "Jest obraz", "Brak sygnału", "Wygaszacz po", "Wygaszacz ekranu", "Uporządkuj aplikacje",
            "Wybierz aplikację OK, przesuń ją, OK ją odkłada. Wstecz: gotowe", "Kafelki", "Oryginalne", "Ciemne",
            "Jasne", "Szkło", "Czarno-białe", "Reflektor", "Pasek stanu", "Zegar", "Data", "Prędkość pobierania",
            "Wyłącznik czasowy", "Taktowanie CPU", "Użycie RAM", "RAM w GB", "Także kolory marek",
            "W dzień jasne, wieczorem ciemne", "Ostatnio używane",
            "Blokada rodzicielska", "Wybierz aplikacje", "Zmień PIN", "Wyłącz blokadę rodzicielską", "Wpisz PIN",
            "Nowy PIN", "Powtórz PIN", "Nieprawidłowy PIN", "Kody PIN się nie zgadzają", "Kopia i przywracanie",
            "Utwórz kopię teraz", "Przywróć", "Kopia utworzona", "Przywrócono", "Nie znaleziono kopii",
            "Gradienty kolorów", "Twoje zdjęcia (USB)", "Nie znaleziono zdjęć",
            "Zdjęcia z telefonu (Wi-Fi)", "Na telefonie lub komputerze w tej samej sieci Wi-Fi otwórz:",
            "Odebrane zdjęcia: %d", "Wybierz zdjęcia", "Wysłano", "Wstecz: gotowe", "Pokaż teraz",
            "Usuń odebrane zdjęcia", "Usunięte zdjęcia: %d",
        });
        LANGS.put("pt", new String[] { // Brazilian Portuguese
            "Configurações", "Adicionar app", "Rede", "Sem conexão", "Som", "Imagem",
            "Mover", "Substituir", "Remover do início", "Desinstalar", "Informações do app",
            "Use as setas para mover e depois aperte OK", "Todos os apps já estão aqui", "Adicionar um app", "Substituir %s", "Plano de fundo", "Configurações da TV", "Brilho",
            "Entrada HDMI", "Todos os apps", "TV ao vivo", "Central de controle", "Todas as configurações",
            "Aberto por último", "agora mesmo", "Nenhum aparelho ligado", "Suspender após", "Desativado", "min",
            "TV desliga em", "A TV desliga em um minuto", "está ligado, aperte OK para assistir", "Com imagem",
            "Sem sinal", "Protetor de tela após", "Protetor de tela", "Organizar apps",
            "Escolha um app com OK, mova, OK solta. Voltar: pronto", "Blocos", "Original", "Escuro", "Claro",
            "Vidro", "Preto e branco", "Destaque", "Barra de status", "Relógio", "Data", "Velocidade de download",
            "Timer de desligamento", "Clock da CPU", "Uso de RAM", "RAM em GB", "Cores da marca também",
            "Claro de dia, escuro à noite", "Usados recentemente",
            "Controle dos pais", "Escolher apps", "Alterar PIN", "Desativar controle dos pais", "Digite o PIN",
            "Novo PIN", "Repita o PIN", "PIN incorreto", "Os PINs não coincidem", "Backup e restauração",
            "Fazer backup agora", "Restaurar", "Backup feito", "Restaurado", "Nenhum backup encontrado",
            "Gradientes de cor", "Suas fotos (USB)", "Nenhuma foto encontrada",
            "Fotos do celular (Wi-Fi)", "No celular ou computador, no mesmo Wi-Fi, abra:", "Fotos recebidas: %d",
            "Escolher fotos", "Enviadas", "Voltar: pronto", "Ver agora", "Apagar fotos recebidas",
            "Fotos apagadas: %d",
        });
        LANGS.put("ru", new String[] {
            "Настройки", "Добавить", "Сеть", "Нет сети", "Звук", "Изображение",
            "Переместить", "Заменить", "Убрать с главного экрана", "Удалить", "О приложении",
            "Перемещайте стрелками, затем нажмите OK", "Все приложения уже здесь", "Добавить приложение", "Заменить %s", "Обои", "Настройки телевизора", "Яркость",
            "Вход HDMI", "Все приложения", "Прямой эфир", "Пункт управления", "Все настройки", "Открыто",
            "только что", "Устройство не включено", "Сон через", "Выкл.", "мин", "ТВ выключится через",
            "ТВ выключится через минуту", "включено, нажмите OK для просмотра", "Есть изображение", "Нет сигнала",
            "Заставка через", "Заставка", "Упорядочить",
            "Выберите приложение кнопкой OK, переместите, OK — поставить. Назад: готово", "Плитки", "Оригинал",
            "Тёмные", "Светлые", "Стекло", "Чёрно-белые", "Прожектор", "Строка состояния", "Часы", "Дата",
            "Скорость загрузки", "Таймер выключения", "Частота CPU", "Загрузка RAM", "RAM в ГБ", "И фирменные цвета",
            "Днём светлые, вечером тёмные", "Недавние",
            "Детский замок", "Выбрать приложения", "Сменить PIN", "Выключить детский замок", "Введите PIN",
            "Новый PIN", "Повторите PIN", "Неверный PIN", "PIN-коды не совпадают", "Резервная копия",
            "Создать копию", "Восстановить", "Копия создана", "Восстановлено", "Копия не найдена",
            "Цветовые переливы", "Ваши фото (USB)", "Фото не найдены",
            "Фото с телефона (Wi-Fi)", "На телефоне или компьютере в той же сети Wi-Fi откройте:",
            "Получено фото: %d", "Выбрать фото", "Отправлено", "Назад: готово", "Показать сейчас",
            "Удалить полученные фото", "Удалено фото: %d",
        });
        LANGS.put("tr", new String[] {
            "Ayarlar", "Uygulama ekle", "Ağ", "Çevrimdışı", "Ses", "Görüntü",
            "Taşı", "Değiştir", "Ana ekrandan kaldır", "Kaldır", "Uygulama bilgisi",
            "Ok tuşlarıyla taşıyın, sonra OK'e basın", "Tüm uygulamalar zaten burada", "Uygulama ekle", "Değiştir: %s", "Duvar kağıdı", "TV ayarları", "Parlaklık",
            "HDMI girişi", "Tüm uygulamalar", "Canlı TV", "Kontrol Merkezi", "Tüm ayarlar", "Son açılış", "az önce",
            "Açık cihaz yok", "Uyku süresi", "Kapalı", "dk", "TV kapanıyor:", "TV bir dakika içinde kapanacak",
            "açık, izlemek için OK'e basın", "Görüntü var", "Sinyal yok", "Ekran koruyucu süresi", "Ekran koruyucu",
            "Uygulamaları düzenle", "OK ile bir uygulama seçin, taşıyın, OK bırakır. Geri: bitti", "Kutucuklar",
            "Orijinal", "Koyu", "Açık", "Cam", "Siyah beyaz", "Spot ışığı", "Durum çubuğu", "Saat", "Tarih",
            "İndirme hızı", "Kapanma zamanlayıcısı", "CPU hızı", "RAM kullanımı", "GB olarak RAM",
            "Marka renkleri de", "Gündüz açık, akşam koyu", "Son kullanılanlar",
            "Çocuk kilidi", "Uygulama seç", "PIN'i değiştir", "Çocuk kilidini kapat", "PIN girin", "Yeni PIN",
            "PIN'i tekrarlayın", "Yanlış PIN", "PIN'ler eşleşmiyor", "Yedekle ve geri yükle", "Şimdi yedekle",
            "Geri yükle", "Yedeklendi", "Geri yüklendi", "Yedek bulunamadı", "Renk geçişleri",
            "Fotoğraflarınız (USB)", "Fotoğraf bulunamadı",
            "Telefondan fotoğraf (Wi-Fi)", "Aynı Wi-Fi'deki telefon veya bilgisayarda açın:", "Alınan fotoğraf: %d",
            "Fotoğraf seç", "Gönderildi", "Geri: bitti", "Şimdi göster", "Alınan fotoğrafları sil",
            "Silinen fotoğraf: %d",
        });
        LANGS.put("uk", new String[] {
            "Налаштування", "Додати додаток", "Мережа", "Офлайн", "Звук", "Зображення",
            "Перемістити", "Замінити", "Прибрати з головного екрана", "Видалити", "Інформація про додаток",
            "Переміщуйте стрілками, а потім натисніть OK", "Усі додатки вже тут", "Додати додаток", "Замінити %s", "Шпалери", "Налаштування телевізора", "Яскравість",
            "Вхід HDMI", "Усі додатки", "Прямий ефір", "Пункт керування", "Усі налаштування", "Відкрито", "щойно",
            "Пристрій не ввімкнено", "Сон через", "Вимк.", "хв", "ТВ вимкнеться через", "ТВ вимкнеться за хвилину",
            "увімкнено, натисніть OK для перегляду", "Є зображення", "Немає сигналу", "Заставка через", "Заставка",
            "Упорядкувати", "Виберіть додаток кнопкою OK, перемістіть, OK — поставити. Назад: готово", "Плитки",
            "Оригінал", "Темні", "Світлі", "Скло", "Чорно-білі", "Прожектор", "Рядок стану", "Годинник", "Дата",
            "Швидкість завантаження", "Таймер вимкнення", "Частота CPU", "Використання RAM", "RAM у ГБ",
            "І фірмові кольори", "Удень світлі, увечері темні", "Нещодавні",
            "Дитячий замок", "Вибрати додатки", "Змінити PIN", "Вимкнути дитячий замок", "Введіть PIN", "Новий PIN",
            "Повторіть PIN", "Неправильний PIN", "PIN-коди не збігаються", "Резервна копія", "Створити копію",
            "Відновити", "Копію створено", "Відновлено", "Копію не знайдено", "Кольорові переливи",
            "Ваші фото (USB)", "Фото не знайдено",
            "Фото з телефона (Wi-Fi)", "На телефоні чи комп'ютері в тій самій мережі Wi-Fi відкрийте:",
            "Отримано фото: %d", "Вибрати фото", "Надіслано", "Назад: готово", "Показати зараз",
            "Видалити отримані фото", "Видалено фото: %d",
        });
        LANGS.put("zh", new String[] { // Simplified Chinese
            "设置", "添加应用", "网络", "未联网", "声音", "图像",
            "移动", "替换", "从主屏幕移除", "卸载", "应用信息",
            "用方向键移动，然后按 OK", "所有应用都已在这里", "添加应用", "替换 %s", "壁纸", "电视设置", "亮度",
            "HDMI 输入", "所有应用", "直播电视", "控制中心", "所有设置", "上次打开", "刚刚", "没有开启的设备", "休眠时间", "关闭", "分钟", "电视关闭倒计时",
            "电视将在一分钟后关闭", "已开启，按 OK 观看", "有画面", "无信号", "屏保时间", "屏幕保护", "排列应用", "按 OK 选中应用，移动后按 OK 放下。返回：完成", "磁贴",
            "原始", "深色", "浅色", "玻璃", "黑白", "聚光灯", "状态栏", "时钟", "日期", "下载速度", "定时关机", "CPU 频率", "内存使用率", "内存 (GB)",
            "品牌色也一起调整", "白天浅色，夜间深色", "最近使用",
            "儿童锁", "选择应用", "更改 PIN 码", "关闭儿童锁", "输入 PIN 码", "新 PIN 码", "再次输入 PIN 码", "PIN 码错误", "两次 PIN 码不一致",
            "备份与恢复", "立即备份", "恢复", "已备份", "已恢复", "未找到备份", "彩色渐变", "你的照片 (USB)", "未找到照片",
            "从手机发送照片 (Wi-Fi)", "在同一 Wi-Fi 下的手机或电脑上打开：", "已收到照片：%d", "选择照片", "已发送", "返回：完成", "立即查看", "删除收到的照片",
            "已删除照片：%d",
        });
        String[] hant = { // Traditional Chinese
            "設定", "新增應用程式", "網路", "離線", "音效", "影像",
            "移動", "取代", "從主畫面移除", "解除安裝", "應用程式資訊",
            "使用方向鍵移動，然後按 OK", "所有應用程式都已在這裡", "新增應用程式", "取代 %s", "桌布", "電視設定", "亮度",
            "HDMI 輸入", "所有應用程式", "直播電視", "控制中心", "所有設定", "上次開啟", "剛剛", "沒有開啟的裝置", "休眠時間", "關閉", "分鐘", "電視關閉倒數",
            "電視將在一分鐘後關閉", "已開啟，按 OK 觀看", "有畫面", "無訊號", "螢幕保護時間", "螢幕保護程式", "排列應用程式", "按 OK 選取應用程式，移動後按 OK 放下。返回：完成",
            "圖塊", "原始", "深色", "淺色", "玻璃", "黑白", "聚光燈", "狀態列", "時鐘", "日期", "下載速度", "定時關機", "CPU 頻率", "記憶體使用率",
            "記憶體 (GB)", "品牌色也一起調整", "白天淺色，夜間深色", "最近使用",
            "兒童鎖", "選擇應用程式", "變更 PIN 碼", "關閉兒童鎖", "輸入 PIN 碼", "新 PIN 碼", "再次輸入 PIN 碼", "PIN 碼錯誤", "兩次 PIN 碼不一致",
            "備份與還原", "立即備份", "還原", "已備份", "已還原", "找不到備份", "彩色漸層", "你的相片 (USB)", "找不到相片",
            "從手機傳送相片 (Wi-Fi)", "在同一 Wi-Fi 的手機或電腦上開啟：", "已收到相片：%d", "選擇相片", "已傳送", "返回：完成", "立即查看", "刪除收到的相片",
            "已刪除相片：%d",
        };
        LANGS.put("zh-TW", hant);
        LANGS.put("zh-HK", hant);
        LANGS.put("zh-MO", hant);
    }

    private Strings() {}

    static String lang() { return Locale.getDefault().getLanguage(); }

    // The label in the TV's language, else the English one. Read on every call, so it follows the TV.
    static String t(String en) {
        Locale l = Locale.getDefault();
        String key = l.getLanguage() + "-" + l.getCountry();
        // Chinese can name its script outright (zh-Hant, zh-Hans-HK): that wins over the country.
        if ("zh".equals(l.getLanguage())) {
            if ("Hant".equals(l.getScript())) key = "zh-TW";
            else if ("Hans".equals(l.getScript())) key = "zh";
        }
        String[] table = LANGS.get(key);
        if (table == null) table = LANGS.get(l.getLanguage());
        if (table == null) return en;
        // A label added to EN but not yet to this table falls back to English.
        for (int i = 0; i < EN.length; i++)
            if (EN[i].equals(en)) return i < table.length ? table[i] : en;
        return en;
    }

    // The date above the clock: "Monday, 19 October" in English, the language's own order elsewhere.
    static String datePattern() {
        if ("en".equals(lang())) return "EEEE, d MMMM";
        return android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM");
    }
}
