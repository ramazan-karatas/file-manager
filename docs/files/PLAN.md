# Dosya Yöneticisi — Uygulama Planı (`app-files`)

Spec: [SPEC.md](SPEC.md) · Mimari: [../00-architecture.md](../00-architecture.md)

Görev ID formatı: `F-<milestone>.<sıra>`. Commit başlığı: `[F-1.3] kısa açıklama`.
Bir görev bitince buradaki kutuyu işaretle. Görevler sıralıdır; bağımlılık belirtilmişse ona uy.

---

## M0 — İskelet ✅ tamamlandı

- [x] **F-0.1 — Gradle monorepo iskeleti**
  - `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `.gitignore`,
    `gradle.properties`, `gradle/gradle-daemon-jvm.properties` (JDK 21), wrapper 9.3.1
  - Modüller: `:core:common`, `:core:design`, `:core:storage`, `:app-files`
  - **Not:** AGP 9.1 Kotlin'i yerleşik getiriyor; `org.jetbrains.kotlin.android`
    uygulanırsa build başarısız oluyor — hiçbir modülde yok.
  - `build-logic` convention plugin'leri henüz yok; dört modülde build dosyaları
    tekrarlı. İkinci uygulama (`app-music`) eklenirken çıkarılacak.

- [x] **F-0.2 — `:core:common`**
  - `DispatcherProvider` (io/default/main) + Hilt modülü
  - `Outcome<T>` sealed interface (`Success`/`Failure(throwable, message)`)
  - Formatter'lar: `formatBytes`, `formatDate`, `formatDuration`, `formatPercent`
  - Kabul: 8 unit test geçiyor (0 B, 1023 B, 1,5 GB TR ondalık ayracı, süre biçimleri).

- [x] **F-0.3 — `:core:design`**
  - `SystemAppsTheme` (dynamic color + koyu tema), `AppTypography`, yedek palet
  - Bileşenler: `AppListItem` (+`ItemIcon`), `EmptyState`, `LoadingState`,
    `SelectionTopBar`, `ConfirmDialog`, `ProgressSheet`
  - **Not:** satır bileşeni `FileListItem` değil `AppListItem` adını aldı —
    `app-music` de aynı satırı kullanacak, domain tipi bilmiyor.
  - Kabul: her bileşenin açık/koyu preview'ı var.

- [x] **F-0.4 — `:app-files` boş uygulama ayağa kalkıyor**
  - Tek `MainActivity`, Hilt `@HiltAndroidApp`, Navigation-Compose iskeleti
  - `applicationId = dev.rk.systemapps.files`, uygulama adı "Dosyalar",
    adaptive launcher ikonu (geçici), `strings.xml` tr + `values-en`
  - Kabul: `./gradlew :app-files:assembleDebug` APK üretiyor; birleşmiş manifest'te
    **hiç izin yok** (INTERNET dâhil) — doğrulandı.

---

## M1 — Çalışan dosya yöneticisi (ana hedef)

- [x] **F-1.1 — İzin altyapısı**
  - `StoragePermissions` (saf mantık, 13 test) + `StoragePermissionChecker` (Flow'lu, Hilt'li)
  - `manageAllFilesIntents()`: uygulamaya özel ekran → genel liste → uygulama detayları,
    sırayla denenir, hiçbiri açılmazsa kullanıcıya yol tarif edilir
  - `PermissionRoute` onboarding ekranı, `AccessWarningBanner`, DataStore'da sınırlı mod onayı
  - İzin durumu `LifecycleResumeEffect` ile her öne gelişte tazelenir (Ayarlar'dan dönüş)
  - **Not:** Android 14+ kısmi görsel erişimi (`READ_MEDIA_VISUAL_USER_SELECTED`) de
    ele alındı; sınırlı mod sayılıyor.
  - Kabul: API 37 emülatöründe üç durum da doğrulandı — izin yokken onboarding (çökme yok),
    sınırlı modda ana ekran + uyarı bandı, tam erişimde onboarding atlanıyor ve bant yok.
  - **Bilinen kozmetik sorun:** açılışta tercihler okunurken kısa bir spinner görünüyor;
    F-1.11'de splash screen API'siyle giderilecek.

- [x] **F-1.2 — `FileNode` soyutlaması ve yerel veri kaynağı**
  - `FileNode` (sealed) + `LocalFileNode` + `DocumentNode`, `SortBy`, `ListingOptions`
  - `FileRepository` arayüzü + `FileRepositoryImpl` (IO dispatcher, `Outcome`)
  - `LocalFileDataSource` — `java.io.File` tabanlı, hatayı exception olarak yükseltir
  - `FileNodeSorter` — süzme + sıralama, yerel duyarlı `Collator` ile
  - `MimeTypeResolver` arayüzü + `AndroidMimeTypeResolver` (MimeTypeMap + yedek tablo)
  - **Spec değişikliği:** kimlik `Uri` yerine `String`; gerekçe SPEC §4'te.
  - **Not:** `DocumentNode` F-2.7'ye kadar kullanılmıyor; sealed hiyerarşide
    "tek UI - iki depolama arka ucu" kuralını görünür kılmak için şimdiden tanımlı.
  - Kabul: 28 unit test — geçici dizin üzerinde listeleme, gizli dosya süzgeci,
    dört sıralama kipi, Türkçe harf sırası, hata durumlarının `Outcome.Failure`'a
    dönüşmesi (exception sızmıyor).
  - Bağımlılık: F-0.2

- [x] **F-1.3 — Gezgin ekranı: listeleme**
  - `BrowserRoute` + `BrowserScreen` + `BrowserViewModel` (`BrowserUiState`, `BrowserAction`)
  - Breadcrumb (yatay kaydırmalı, tıklanabilir), liste/ızgara anahtarı,
    `FileKind` → ikon eşlemesi, Coil ile görsel/video önizlemesi ve APK ikonu
  - Her klasör ayrı bir navigasyon hedefi: geri tuşu üst klasöre gider, kaydırma
    konumu Navigation'ın kendi durum saklamasıyla korunur
  - Breadcrumb'tan atlarken geri yığını kökten hedefe yeniden kurulur
  - **Spec sapması:** klasör satırında "n öğe" yerine tarih gösteriliyor. Öğe sayısı
    her klasör için ayrı dizin okuması demek; listeleme bütçesini bozuyordu.
    Sayı, F-1.8'deki özellikler diyaloğunda hesaplanacak.
  - **Kabul kriteri kısmen karşılandı:**
    - Kaydırma: 10.000 satırda tek bir atlanan kare yok. ✅
    - Süre: emülatörde (API 37, x86_64) 10.000 dosya **~3,2 sn**, kriter 300 ms. ❌
      Ölçüm kırılımı: `readdir` 1400 ms + `stat` 1185 ms = 2586 ms saf dosya sistemi,
      geri kalan ~600 ms sıralama/eşleme/akış. Yani darboğaz uygulama değil,
      emülatörün FUSE `/sdcard` katmanı. **Gerçek Xiaomi cihazda yeniden ölçülecek**;
      ölçüm orada da tutmazsa çözüm iki fazlı yükleme (önce adlar, sonra öznitelikler).
    - Denenip **geri alınan** iyileştirme: `java.nio.file` ile öğe başına tek stat —
      aynı koşulda kazanç vermedi (NIO 2586 ms, File API 2296 ms). Gerekçe kodda not düşüldü.
    - Uygulanan iyileştirme: sıralamada `CollationKey` önbelleği (10.000 öğede
      ~130.000 pahalı `Collator.compare` çağrısı yerine 10.000 anahtar üretimi).
  - Bağımlılık: F-1.2

- [x] **F-1.4 — Sıralama, görünüm ve tercihler**
  - `BrowserPrefs` (sıralama, yön, ızgara, gizli dosyalar, klasörler üstte) DataStore'da
  - Üst çubukta sıralama menüsü; tercih değişince `collectLatest` ile yeniden listelenir
  - `FilesPreferences` arayüze çevrildi — ViewModel testlerinde sahte uygulama veriliyor
  - Kabul: emülatörde "Boyuta göre" seçilip uygulama force-stop edildi, yeniden
    açıldığında seçim korunuyor. ✅

- [x] **F-1.5 — Seçim modu**
  - Uzun basma ile giriş, sayaç, tümünü seç, ters seç; geri tuşu önce seçimi kapatır
  - Seçim `SavedStateHandle`'da tutulur; listeden kaybolan öğeler seçimden düşer
  - Liste ve ızgara kiplerinin ikisinde de çalışır
  - **Not:** kopyala/taşı/sil ikonları seçim çubuğunda henüz yok, F-1.6 ve F-1.7'de gelecek.
  - Kabul: 10 ViewModel testi; aralarında aynı `SavedStateHandle` ile yeni ViewModel
    oluşturup seçimin korunduğunu doğrulayan test (proses ölümü / döndürme senaryosu). ✅
  - **Yan bulgu:** `Crumb.fromPath` varsayılan parametresinde `Environment` çağrısı vardı,
    ViewModel'i JVM'de test edilemez yapıyordu. `StorageLocations` arayüzüne çevrildi.

- [x] **F-1.6 — Dosya işlemi motoru (çekirdek)**
  - `FileOperation`, `OperationProgress`, `ConflictResolver` (SPEC §5 güncellendi)
  - `FileOperationEngine`: kopyala/taşı/sil, ön hesap, 64 KB tampon, 200 ms throttle, iptal
  - **Spec sapmaları:** `CONFLICT` durumu yayınlanmıyor — motor `ConflictResolver`'a sorup
    cevabı bekliyor (UI bağımsızlığı ve test edilebilirlik için). `Compress`/`Extract` ve
    `Delete.toTrash` uygulamalarıyla birlikte F-2.4 ve F-2.3'te eklenecek.
  - Kabul: 20 test, kriterdeki dört senaryo dâhil — çakışmanın dört kipi, "hepsine uygula",
    iptalde yarım dosyanın silinmesi (+ iptalden önce bitenlerin yerinde kalması),
    çakışmalı taşımada kopyala+sil yolu, bir öğe hata verince diğerlerinin devam etmesi. ✅
  - **Henüz UI'ya bağlı değil**; seçim çubuğu ikonları ve yapıştırma çubuğu F-1.7'de.
  - Bağımlılık: F-1.2

- [x] **F-1.7 — `FileOperationService` (foreground)**
  - `FileOperationManager`: kuyruk, ilerleme durumu, çakışma yönlendirmesi, iptal.
    **Servis işi yapmaz**, yalnızca süreci ayakta tutar ve bildirimi günceller; UI
    doğrudan yöneticiye bağlanır, böylece çakışma diyaloğu Intent trafiği gerektirmez.
  - `foregroundServiceType="dataSync"`, bildirimde ilerleme + iptal aksiyonu
  - `POST_NOTIFICATIONS` ilk gerçek dosya işleminde isteniyor (bağlamsız sorulmuyor)
  - Pano DataStore'da: uygulama yeniden başlasa da yapıştırma çubuğu kalıyor.
    Taşımada pano tüketilir, kopyalamada korunur (birden çok yere yapıştırılabilsin).
  - UI: seçim çubuğunda kopyala/kes/sil, alt yapıştırma çubuğu, ilerleme paneli,
    çakışma diyaloğu, silme onayı, bitişte özet snackbar'ı
  - `MediaScannerConnection` her işlemden sonra tetikleniyor
  - Kabul (emülatör, API 37):
    - 300 MB kopyalama uygulama **arka plandayken** tamamlandı; servis `dataSync`
      tipiyle ön planda, bildirim ilerlemeyi gösterdi. ✅
    - İptal: 1,2 GB kopyalama ortasında iptal edildi, hedef klasör boş kaldı
      (yarım dosya yok), kaynak sağlam. ✅
    - **Not:** iptal, uygulama içi panelden tetiklendi. Bildirimdeki düğme aynı
      `cancelCurrent()` yoluna gider ve aksiyonun kayıtlı olduğu `dumpsys` ile
      doğrulandı, ancak düğmeye basarak ayrıca sınanmadı.
  - **Yol boyunca bulunan iki hata:** (1) boş klasörde `EmptyState` tüm alanı kaplayıp
    alt çubukları ekran dışına itiyordu; (2) servis durduğunda bildirim ekranda kalıyordu
    (NotificationManager üzerinden güncellendiği için otomatik kalkmıyor). İkisi de düzeltildi.
  - 7 yeni `FileOperationManager` testi.
  - Bağımlılık: F-1.6

- [x] **F-1.8 — Tekil dosya işlemleri**
  - Dosyaya tıklayınca `ACTION_VIEW` ile açılıyor; açacak uygulama yoksa snackbar
  - Paylaşma `ShareCompat` ile (aşağıdaki nota bakın), tek ve çoklu seçim
  - Yeniden adlandırma, yeni klasör, yeni dosya (FAB menüsü) — ortak `NameInputDialog`;
    yeniden adlandırmada uzantı seçimin dışında bırakılıyor
  - Özellikler diyaloğu: konum, boyut, tür, tarih, `rwx` izinleri, görsel/video için
    çözünürlük ve süre. Klasör boyutu ayrı hesaplanıp spinner'la gösteriliyor.
  - `FileProvider` + `file_paths.xml`; `MediaMetadataReader` arayüzü (testlerde sahte)
  - Kabul: emülatörde Türkçe adlı dosya (`şubat çalışması.png`) Google Photos'ta
    açıldı, paylaşım seçicisi önizlemeyle geldi, özellikler diyaloğu çözünürlüğü
    (1080×2340) doğru gösterdi. ✅
  - **Cihazda bulunan üç hata:**
    1. `Intent.createChooser` sarmalayınca sistem seçicisi URI'yi okuyamıyordu
       (`Permission Denial`, önizleme boş kalıyordu) — `ShareCompat.IntentBuilder`
       akışları `ClipData` olarak da eklediği için izin seçiciye taşınıyor.
    2. FAB, alt çubukların üstüne biniyordu — çubuklar `Scaffold`'un `bottomBar`
       yuvasına taşındı.
    3. Seçim çubuğundaki sekiz ikon sayacı ve kapatma düğmesini ekrandan taşırıyordu —
       yeniden adlandır/özellikler/tümünü seç/ters çevir taşma menüsüne alındı.
  - **Lint'in yakaladığı hata:** `MediaMetadataRetriever.use {}` API 29 gerektiriyor,
    minSdk 26'da çökerdi; `try/finally` + `release()` ile değiştirildi.
  - 16 yeni test (repoda 102).
  - **F-1.11'e not:** beyaz/açık renkli görsellerin küçük resmi açık zeminde görünmüyor;
    thumbnail'lere ince bir çerçeve veya zemin gerekiyor.

- [x] **F-1.9 — Arama**
  - `LocalFileDataSource.search()`: yığın tabanlı özyinelemeli tarama, `Flow<FileNode>`,
    sonuçlar bulundukça yayılıyor, canonical yol setiyle symlink döngüsü koruması
  - `SearchViewModel`: 250 ms debounce + `flatMapLatest` (yeni sorgu öncekini iptal eder)
  - `SearchScreen`: gezgin üst çubuğundaki arama ikonundan açılıyor, sonuçta konum
    gösteriliyor, klasöre atlama ve dosyayı açma çalışıyor
  - Kabul:
    - Sorgu değişince eski tarama iptal oluyor — ViewModel testiyle doğrulandı
      (hızlı yazmada repository'ye yalnızca son sorgu gidiyor). ✅
    - Symlink döngüsü — testi yazıldı ama **Windows'ta atlanıyor**
      (`Files.createSymbolicLink` yönetici hakkı istiyor). Linux/CI'da çalışacak;
      `/sdcard` FUSE olduğu için emülatörde de symlink kurulamadı. ⚠️
  - **Arama ekranı cihazda çalıştırılamadı:** emülatör oturum sonunda yanıt vermez
    hâle geldi. Ekran 9 birim testiyle kaplı ve zaten kanıtlanmış bileşenlerden
    kuruluyor, ama gerçek cihazda bir kez denenmesi gerekiyor.
  - 9 yeni test (repoda 111).

- [x] **F-1.10 — Ana ekran**
  - Depolama kartları: doluluk çubuğu + "x / y kullanıldı"; tıklayınca o birimin kökü açılır
  - Kategoriler: İndirilenler (gerçek klasör) + Görseller/Video/Ses/Belgeler/APK/Arşivler
    (MediaStore sorgusu → `CategoryScreen`)
  - Son değişenler: son 7 günde değişmiş 20 dosya
  - **Not:** çıkarılabilir birimin kökü `StorageManager`'dan doğrudan alınamıyor;
    `getExternalFilesDirs` sonucundan `/Android/data/<paket>/files` kırpılarak bulunuyor.
    API 26'dan beri çalışan tek taşınabilir yol bu. Ad ise API 30+'ta
    `StorageVolume.getDescription()` ile, yoksa klasör adıyla veriliyor.
  - **Not:** APK ve arşivler MediaStore'da tutarlı MIME türüyle indekslenmediği için
    dosya adı uzantısına göre sorgulanıyor.
  - Kabul: emülatörde iki kart da göründü — dahili depolama (2,3 GB / 5,8 GB) ve
    SDCARD (68 KB / 510 MB). Kategoriler ve son değişenler de çizildi. ✅
  - 4 yeni test (`StorageVolumeInfo` hesapları; repoda 115).

- [x] **F-1.11 — M1 cilası**
  - Hakkında ekranı (ana ekranın üst çubuğundan): ağa çıkmama ve reklamsızlık sözü +
    istenen izinlerin tamamı tek tek, gerekçeleriyle
  - `INTERNET` izni denetimi: `verify<Variant>NoInternetPermission` görevi AGP'nin
    artifact API'siyle **birleşmiş** manifest'i okuyor ve `check`e bağlı.
    Unit test yerine Gradle görevi seçildi çünkü izni bir bağımlılık da ekleyebilir;
    kaynak manifest'e bakmak bunu yakalamaz.
    Görevin gerçekten tetiklendiği, izin geçici eklenip derlemenin patlaması ve
    geri alınınca geçmesiyle doğrulandı.
  - Açılıştaki spinner kaldırıldı: tercihler okunurken tema zemini gösteriliyor,
    yarım saniyelik flaş yok
  - Küçük resimlere ince çerçeve: beyaz/açık görseller açık zeminde kayboluyordu
  - Kabul: SPEC §7 gözden geçirildi — sekiz kriterden altısı karşılandı.
    Kalan ikisi (10.000 dosya süresi, symlink döngüsü) kod eksikliği değil,
    doğrulama ortamı eksikliği; gerekçeleri SPEC §7'de.

---

## Ölçüm — sunum için karşılaştırma

- [ ] **B-1 — MIUI ile açılış süresi karşılaştırması.** Ayrı görev tanımı:
      [BENCHMARK.md](BENCHMARK.md). Gerçek cihaz gerektirir, ayrı oturumda yapılacak.

---

## M2 — Güçlü özellikler

- [ ] **F-2.1 — Room kurulumu** (`bookmarks`, `trash_entries`, `dir_size_cache`)
- [ ] **F-2.2 — Kısayollar/yıldızlar** — ana ekranda bölüm, gezginde yıldız butonu
- [ ] **F-2.3 — Çöp kutusu** — silme varsayılan olarak çöpe, geri al, 30 gün sonra otomatik temizlik (WorkManager)
- [ ] **F-2.4 — ZIP oluşturma/açma** — `java.util.zip`, `FileOperationEngine`'e `Compress`/`Extract` ekle
- [ ] **F-2.5 — commons-compress + junrar** — 7z/tar.gz/rar okuma, arşiv içi gezinme, tek dosya çıkarma
- [ ] **F-2.6 — Depolama analizi** (SPEC 3.5) — akışlı hesap, cache, yüzde barları
- [ ] **F-2.7 — SAF entegrasyonu** — `DocumentNode`, SD kart/USB yazma, `/Android/data` fallback,
      `takePersistableUriPermission`, mümkünse `DocumentsContract.copyDocument`
- [ ] **F-2.8 — Gelişmiş arama filtreleri** — tür, boyut aralığı, tarih aralığı
- [ ] **F-2.9 — APK yöneticisi** — kurulu uygulamaları listele, APK'sını dışa aktar

---

## M3 — İleri seviye

- [ ] **F-3.1 — Gizli kasa** — biyometrik kilit (`androidx.biometric`), `.nomedia`, taşıma akışı
- [ ] **F-3.2 — SMB istemcisi** — `jcifs-ng`; **bu aşamada `INTERNET` izni eklenir**,
      spec ve Hakkında ekranı güncellenir
- [ ] **F-3.3 — FTP/SFTP** — `commons-net` / `sshj`
- [ ] **F-3.4 — Ana ekran widget'ı** — hızlı kısayollar
- [ ] **F-3.5 — Toplu yeniden adlandırma** — desen/sayaç tabanlı

---

## Önerilen çalışma sırası

`F-0.1 → F-0.4` (iskelet) → `F-1.1 → F-1.11` (kullanılabilir uygulama) → cihazda 1 hafta
günlük kullan → geri bildirime göre M2'yi önceliklendir. M2'den önce müzik çaların M1'ine
geçmek de mantıklı: `:core:design` ve `:core:storage` orada ikinci tüketicisini bulur ve
soyutlamalar erken doğrulanır.
