# Müzik Çalar — Spec (`app-music`)

`applicationId`: `dev.rk.systemapps.music` · Uygulama adı: **Müzik**

## 1. Amaç ve kapsam

Mi Music'in reklamsız yerine geçmek: **yalnızca yerel dosyaları** çalan, ağa çıkmayan,
hızlı açılan bir çalar. MIUI'nin yapamadığı iki şey ayrıca hedefleniyor:
**klasör bazlı gezinme** ve **gerçek gapless oynatma**.

**Kapsam dışı:** streaming, çevrimiçi sözler/kapak indirme, hesap, öneri, podcast (şimdilik),
video oynatma.

## 2. Kullanıcı hikâyeleri

| # | Hikâye | Milestone |
|---|---|---|
| U1 | Telefondaki tüm şarkıları görüp çalabilmeliyim | M1 |
| U2 | Albüm/sanatçı/klasöre göre gezinebilmeliyim | M1 |
| U3 | Kilit ekranından ve kulaklık tuşundan kontrol edebilmeliyim | M1 |
| U4 | Kulaklığı çıkarınca müzik durmalı | M1 |
| U5 | Karıştır ve tekrar modlarını kullanabilmeliyim | M1 |
| U6 | Sıradaki şarkıları görüp sırayı düzenleyebilmeliyim | M1 |
| U7 | Uygulamayı kapatıp açınca kaldığım yerden devam etmeliyim | M1 |
| U8 | Kendi çalma listelerimi oluşturabilmeliyim | M2 |
| U9 | Ekolayzer kullanabilmeliyim | M2 |
| U10 | Uyku zamanlayıcısı kurabilmeliyim | M2 |
| U11 | Şarkılar arasında boşluk olmamalı (canlı albümler) | M2 |
| U12 | Etiketleri (sanatçı/albüm adı) düzeltebilmeliyim | M3 |
| U13 | Ana ekran widget'ından kontrol edebilmeliyim | M3 |
| U14 | `.lrc` sözlerini görebilmeliyim | M3 |

## 3. Mimari — oynatma katmanı

Media3 `MediaLibraryService` merkezli. **Kural: ExoPlayer örneği yalnızca serviste yaşar.**
UI, `MediaController` ile bağlanır; ViewModel'ler ExoPlayer'a doğrudan erişmez.

```
MusicService : MediaLibraryService
   ├─ ExoPlayer                 (tek örnek)
   ├─ MediaSession              (bildirim, kilit ekranı, Bluetooth, Android Auto)
   ├─ PlaybackStateWriter       (her 5 sn'de bir konumu DataStore'a yaz → devam ettirme)
   └─ MediaLibrarySessionCallback (Android Auto / Assistant için ağaç)

UI tarafı:
   PlayerConnection (singleton) — MediaController'ı tutar, durumu Flow olarak yayar
   ├─ playbackState: StateFlow<PlaybackUiState>
   └─ queue: StateFlow<List<Track>>
```

`MediaLibraryService` (sadece `MediaSessionService` değil) seçilmesinin sebebi:
Android Auto ve sesli asistan desteği M3'te neredeyse bedava gelir.

Ek zorunluluklar:

- **Audio focus:** Media3'ün `setAudioAttributes(..., handleAudioFocus = true)` kullanılır;
  telefon geldiğinde duraklama, bittiğinde devam.
- **Becoming noisy:** `setHandleAudioBecomingNoisy(true)` — kulaklık çıkınca duraklat (U4).
- **Foreground service:** `foregroundServiceType="mediaPlayback"`.
- **HyperOS:** kullanıcıdan "Otomatik başlat" + pil kısıtlaması muafiyeti istenir
  (Ayarlar ekranında açıklamalı kısayol), yoksa servis öldürülebilir.

## 4. Kütüphane ve veri modeli

### 4.1 Tarama stratejisi

MediaStore doğrudan sorgulanmaz — **Room'a cache'lenir**. Sebep: klasör ağacı,
albüm gruplaması ve arama MediaStore sorgularıyla yavaş; ayrıca kendi
alanlarımızı (çalma sayısı, favori) ekleyebilmeliyiz.

- İlk açılışta tam tarama (ilerleme göstergeli).
- Sonrasında `ContentObserver` ile MediaStore değişikliği dinlenir → artımlı tarama
  (`DATE_MODIFIED` > son tarama zamanı olanlar).
- Manuel "yeniden tara" butonu (Ayarlar).
- **Filtreler:** süresi < 30 sn olanlar ve zil sesi/bildirim/alarm klasörleri
  varsayılan olarak gizlenir; ayardan açılabilir. Kullanıcı klasör bazlı
  dışlama listesi tanımlayabilir (`.nomedia` ayrıca saygı görür).

### 4.2 Room şeması

```kotlin
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,          // MediaStore _ID
    val title: String,
    val artist: String?,
    val albumId: Long?,
    val album: String?,
    val albumArtist: String?,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val path: String,                  // tam dosya yolu
    val parentFolder: String,          // klasör gezinme için indexli
    val sizeBytes: Long,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
)

@Entity(tableName = "playlists")   data class PlaylistEntity(id, name, createdAt, updatedAt)
@Entity(tableName = "playlist_tracks") // playlistId, trackId, position  (composite PK)
@Entity(tableName = "play_stats")  data class PlayStatsEntity(trackId, playCount, lastPlayedAt, isFavorite)
@Entity(tableName = "folder_excludes") data class FolderExcludeEntity(path)
```

`parentFolder` ve `albumId` indexli. FTS gerekmiyor; `LIKE` sorgusu 5.000 şarkıda yeterli
(daha büyük kütüphanede FTS4 tablosu eklenir — açık soru).

### 4.3 Albüm kapağı

Öncelik sırası: (1) dosyaya gömülü kapak (`MediaMetadataRetriever.embeddedPicture`),
(2) klasördeki `cover.jpg`/`folder.jpg`/`albumart.jpg`, (3) MediaStore albüm art URI'si,
(4) placeholder. Coil için özel `Fetcher` yazılır; disk cache açık.
**İnternetten kapak indirme yok.**

## 5. Ekranlar

### 5.1 Kütüphane (`LibraryScreen`) — sekmeli
Sekmeler: **Şarkılar · Albümler · Sanatçılar · Klasörler · Çalma Listeleri**
(sıra ve görünürlük ayardan değiştirilebilir).

- Şarkılar: alfabetik hızlı kaydırma harfi (fast scroll), sıralama (ad/tarih/süre/sanatçı).
- Albümler/Sanatçılar: ızgara, kapaklı.
- **Klasörler:** gerçek dizin ağacı; bir klasörü "tümünü çal" ile kuyruğa alma.
- Üstte arama (şarkı/albüm/sanatçı, tek alanda).

### 5.2 Şimdi çalıyor (`NowPlayingScreen`)
- Tam ekran: büyük kapak, başlık/sanatçı, seekbar (geçen/kalan süre),
  önceki/oynat-duraklat/sonraki, karıştır, tekrar (kapalı/tümü/tek), favori.
- Alt kısımda mini kuyruk kulpu → kuyruk sayfası.
- Mini oynatıcı: kütüphane ekranının altında sabit; yukarı sürükleyince tam ekrana açılır.

### 5.3 Kuyruk (`QueueScreen`)
- Sürükle-bırak sıralama, kaydırarak çıkarma, "sıradaki çal" ile eklenenler ayrı işaretli.

### 5.4 Ayarlar
Tema (sistem/açık/koyu + dynamic color), sekme düzeni, kısa parça filtresi eşiği,
dışlanan klasörler, yeniden tara, ekolayzer (M2), uyku zamanlayıcısı (M2),
"Hakkında" (internet izni yok bilgisi + izin listesi).

## 6. Oynatma davranış kuralları

1. **Kaldığı yerden devam:** son kuyruk + indeks + konum DataStore'a yazılır;
   açılışta yüklenir ama **otomatik çalmaz** (duraklatılmış durumda hazır).
2. **Kuyruk kaynağı korunur:** bir albümden çalmaya başlayınca kuyruk o albümdür;
   karıştır açılırsa orijinal sıra saklanır, kapatılınca geri döner.
3. **Sıradaki çal / kuyruğa ekle** ayrımı korunur.
4. Dosya silinmiş/erişilemezse hata gösterilip sonraki parçaya geçilir (takılıp kalma yok).
5. Kulaklık tuşu: tek tık oynat/duraklat, çift tık sonraki, üç tık önceki.
6. **Gapless (M2):** Media3 varsayılan olarak destekler; MP3'lerde LAME etiketi varsa
   otomatik çalışır. Aynı albümdeki parçalar tek `MediaItem` listesi olarak verilmeli
   (her parçada yeni player oluşturma yok).

## 7. Kabul kriterleri — M1 "bitti" tanımı

- [ ] Soğuk açılış → kütüphane görünür < 1 sn (3.000 şarkılı cihazda).
- [ ] İlk tam tarama 3.000 şarkıda < 20 sn, ilerleme gösteriyor, arka planda kalmıyor.
- [ ] Ekran kapalıyken 30 dk kesintisiz çalma (HyperOS'ta servis öldürülmüyor).
- [ ] Kilit ekranı kontrolleri, kapak ve seekbar doğru çalışıyor.
- [ ] Kulaklık çıkarılınca duraklıyor; telefon gelince duraklayıp bitince devam ediyor.
- [ ] Uygulama öldürülüp açıldığında aynı kuyruk ve konum geri geliyor.
- [ ] Bluetooth hoparlörde başlık/sanatçı bilgisi doğru görünüyor.
- [ ] `INTERNET` izni manifest'te yok — bunu doğrulayan bir test var.

## 8. Bilinen riskler

| Risk | Azaltma |
|---|---|
| HyperOS servisi arka planda öldürüyor | `mediaPlayback` foreground type + kullanıcıya otomatik başlat/pil muafiyeti rehberi |
| MediaStore bazı dosyaları hiç indekslemiyor | Ayarlarda "klasörden tara" seçeneği (doğrudan dosya sistemi taraması, M2) |
| Gömülü kapak okuma ana thread'i kilitler | Coil Fetcher IO dispatcher'da, disk cache'li |
| AudioEffect ekolayzer bazı cihazlarda desteklenmiyor | Desteklenmiyorsa ekolayzer menüsü gizlenir, çökme yok |
| Büyük kuyruk (5.000+) `MediaController` üzerinden yavaş aktarılıyor | Kuyruk sayfalı gönderilir / `setMediaItems` tek seferde, UI listesi Room'dan okunur |

## 9. Açık Sorular

- [ ] Etiket düzenleme hangi kütüphane ile? (jaudiotagger Android'de sorunlu; aday: taglib NDK binding — M3'te karar)
- [ ] 10.000+ şarkılı kütüphane için Room FTS4 gerekli mi? (varsayım: şimdilik hayır)
- [ ] Çalma listeleri MediaStore'a da yazılsın mı (başka uygulamalar görsün diye)?
      (varsayım: hayır, MediaStore playlist API'si API 30'da deprecate edildi; m3u dışa aktarma yeterli)
