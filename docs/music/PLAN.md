# Müzik Çalar — Uygulama Planı (`app-music`)

Spec: [SPEC.md](SPEC.md) · Mimari: [../00-architecture.md](../00-architecture.md)

Görev ID formatı: `M-<milestone>.<sıra>`. Commit başlığı: `[M-1.4] kısa açıklama`.
Bir görev bitince kutuyu işaretle.

**Önkoşul:** `:core:common` ve `:core:design` hazır olmalı (bkz. `../files/PLAN.md`
görevleri F-0.2, F-0.3). Dosya yöneticisiyle paralel çalışılıyorsa o modüller önce bitmeli.

---

## M0 — İskelet (yarım gün)

- [ ] **M-0.1 — `:app-music` modülü**
  - `settings.gradle.kts`'e ekle; `applicationId = dev.rk.systemapps.music`, adı "Müzik"
  - Hilt, Compose, Navigation, `:core:common`, `:core:design` bağlantıları
  - Version catalog'a media3 (exoplayer, session, ui) ve room ekle
  - Kabul: boş uygulama cihaza kuruluyor.

- [ ] **M-0.2 — İzinler ve onboarding**
  - `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (≤32), `POST_NOTIFICATIONS`
  - İzin yoksa açıklamalı ekran; reddedilse bile çökme yok
  - HyperOS rehberi: "Otomatik başlat" ve pil kısıtlaması muafiyeti kısayolları (Ayarlar'da)

---

## M1 — Çalışan müzik çalar (ana hedef)

- [ ] **M-1.1 — Room şeması**
  - `TrackEntity`, `PlayStatsEntity`, `FolderExcludeEntity` + DAO'lar (SPEC 4.2)
  - `parentFolder`, `albumId`, `artist` indexleri
  - Kabul: DAO testleri (in-memory Room) — albüm gruplaması, klasör sorgusu, arama.

- [ ] **M-1.2 — MediaStore tarayıcı**
  - `MediaStoreAudioDataSource` → `TrackEntity` eşlemesi
  - Tam tarama + `DATE_MODIFIED` tabanlı artımlı tarama; silinenlerin temizliği
  - Kısa parça filtresi (< 30 sn), zil/alarm/bildirim klasörleri hariç, `.nomedia` saygısı
  - İlerleme akışı (`Flow<ScanProgress>`)
  - Kabul: 3.000 şarkı < 20 sn; ikinci tarama (değişiklik yokken) < 1 sn.
  - Bağımlılık: M-1.1

- [ ] **M-1.3 — `ContentObserver` ile otomatik güncelleme**
  - MediaStore audio URI'si dinlenir, debounce'lu artımlı tarama tetiklenir
  - Kabul: yeni bir mp3 kopyalanınca uygulama yeniden açılmadan listede beliriyor.

- [ ] **M-1.4 — `MusicService` (Media3 `MediaLibraryService`)**
  - Tek `ExoPlayer` örneği, `MediaSession`, `foregroundServiceType="mediaPlayback"`
  - `setAudioAttributes(handleAudioFocus = true)`, `setHandleAudioBecomingNoisy(true)`
  - Bildirim: `MediaNotification` (kapak, başlık, önceki/oynat/sonraki)
  - Kabul: ekran kapalıyken 30 dk kesintisiz çalıyor; kulaklık çıkınca duruyor;
    telefon gelince duraklayıp bitince devam ediyor.

- [ ] **M-1.5 — `PlayerConnection` (UI ↔ servis köprüsü)**
  - `MediaController` bağlantısı (singleton, `@ActivityRetainedScoped` değil — uygulama ömrü)
  - `playbackState: StateFlow<PlaybackUiState>`, `queue: StateFlow<List<Track>>`
  - Komutlar: play/pause/seek/next/prev/shuffle/repeat/setQueue/playNext/addToQueue
  - Kabul: ViewModel'lerde hiçbir yerde `ExoPlayer` referansı yok (mimari testi/grep).
  - Bağımlılık: M-1.4

- [ ] **M-1.6 — Albüm kapağı altyapısı**
  - Coil 3 için özel `Fetcher`: gömülü kapak → klasör kapağı → MediaStore → placeholder (SPEC 4.3)
  - Disk + bellek cache; IO dispatcher'da
  - Kabul: 100 albümlük ızgarada kaydırma takılmıyor.

- [ ] **M-1.7 — Kütüphane ekranı: Şarkılar sekmesi**
  - `LibraryRoute`/`LibraryScreen`, sekme iskeleti, şarkı listesi, sıralama, fast-scroll harfi
  - Satıra tıklama → o listeyi kuyruk yapıp çalmaya başla
  - Kabul: soğuk açılış → liste görünür < 1 sn (3.000 şarkı).
  - Bağımlılık: M-1.1, M-1.5

- [ ] **M-1.8 — Albümler, Sanatçılar, Klasörler sekmeleri**
  - Albüm/sanatçı ızgarası + detay ekranları; klasör ağacı gezinme, "klasörü çal"
  - Kabul: klasör sekmesi gerçek dizin hiyerarşisini gösteriyor (düz liste değil).

- [ ] **M-1.9 — Mini oynatıcı + Şimdi Çalıyor ekranı**
  - Alt sabit mini player, yukarı sürükleyince tam ekran (`BottomSheetScaffold` veya özel)
  - Seekbar, karıştır, tekrar (3 mod), favori butonu
  - Kabul: seekbar sürüklerken takılma yok; kapak geçişi akıcı.

- [ ] **M-1.10 — Kuyruk ekranı**
  - Sürükle-bırak sıralama, kaydırarak çıkarma, "sıradaki" işaretlemesi
  - Karıştır açılınca orijinal sıranın saklanması (SPEC 6.2)
  - Kabul: karıştır aç-kapat sonrası orijinal sıra birebir dönüyor.

- [ ] **M-1.11 — Durumu kalıcılaştırma**
  - Kuyruk + indeks + konum 5 sn'de bir DataStore'a; açılışta yükle, **otomatik çalma yok**
  - Kabul: uygulama force-stop edilip açılınca aynı parça ve konum hazır.

- [ ] **M-1.12 — Arama**
  - Tek alanda şarkı/albüm/sanatçı araması (Room `LIKE`), debounce, gruplu sonuç
  - Kabul: 3.000 şarkıda sonuç < 150 ms.

- [ ] **M-1.13 — M1 cilası**
  - Boş durumlar, hata mesajları, `strings.xml` (tr + `values-en`), tema ayarı
  - Hakkında ekranı: internet izni yok, reklam yok, izin listesi
  - `INTERNET` izninin yokluğunu doğrulayan test
  - Kabul: SPEC 7'deki tüm kabul kriterleri işaretli.

---

## M2 — Günlük kullanımda eksik hissedilenler

- [ ] **M-2.1 — Çalma listeleri** — `PlaylistEntity` + `playlist_tracks`, oluştur/düzenle/sırala
- [ ] **M-2.2 — m3u içe/dışa aktarma** — göreli yol desteği
- [ ] **M-2.3 — Favoriler ve çalma istatistikleri** — "En çok çalınanlar", "Son eklenenler" otomatik listeleri
- [ ] **M-2.4 — Ekolayzer** — `AudioEffect` + preset'ler, bass boost, virtualizer;
      desteklenmiyorsa menü gizlenir
- [ ] **M-2.5 — Uyku zamanlayıcısı** — süre veya "parça bitince dur", ses kısarak sonlanma
- [ ] **M-2.6 — Gapless doğrulama** — aynı albüm tek `MediaItem` listesi, canlı albümle test
- [ ] **M-2.7 — Crossfade** — Media3'te manuel; opsiyonel, kullanıcı ayarı
- [ ] **M-2.8 — Klasörden doğrudan tarama** — MediaStore'un atladığı dosyalar için
      dosya sistemi tabanlı tarama seçeneği
- [ ] **M-2.9 — Dışlanan klasör yönetimi** — ayarlar ekranından klasör seçimi

---

## M3 — İleri seviye

- [ ] **M-3.1 — Ana ekran widget'ı** (2×2 ve 4×2, Glance)
- [ ] **M-3.2 — Android Auto** — `MediaLibrarySessionCallback` ağacı zaten hazır, doğrulama + test
- [ ] **M-3.3 — `.lrc` söz desteği** — dosya yanındaki/gömülü sözler, senkron kaydırma
- [ ] **M-3.4 — Etiket düzenleyici** — kütüphane kararı gerekli (Açık Sorular)
- [ ] **M-3.5 — ReplayGain** — gömülü etiketten ses seviyesi normalizasyonu
- [ ] **M-3.6 — Kısayollar** — uzun basma menüsü (son çalınan, karıştır)

---

## Önerilen çalışma sırası

`M-0.1 → M-1.5` bittiğinde elinde "kütüphaneyi tarayan ve arka planda kesintisiz çalan"
bir çekirdek olur — asıl riskli kısım budur, önce onu bitir. UI sekmeleri (M-1.7 → M-1.10)
ondan sonra hızlı ilerler.

**Dosya yöneticisiyle sıralama:** `app-files` M1'i bitirip günlük kullanıma aldıktan sonra
buraya geç. `:core:design` ve `:core:common` ikinci tüketicisini burada bulur; o sırada
ortaya çıkan tekrarları `core`'a taşı (CLAUDE.md kural 5: en az iki tüketici).
