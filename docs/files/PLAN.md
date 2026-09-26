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

- [ ] **F-1.1 — İzin altyapısı**
  - `StoragePermissionManager` (`:core:storage`): API'ye göre hangi izin gerekli, durum Flow'u
  - `MANAGE_EXTERNAL_STORAGE` intent'i + MIUI fallback (try/catch → `ACTION_APPLICATION_DETAILS_SETTINGS`)
  - Onboarding ekranı (SPEC 3.7), izin yoksa "sınırlı mod" bayrağı
  - Kabul: izin reddedilince çökme yok, üstte uyarı bandı görünüyor.

- [ ] **F-1.2 — `FileNode` soyutlaması ve yerel veri kaynağı**
  - `domain/model/FileNode.kt` (SPEC 4), `LocalFileNode`, `DocumentNode`
  - `FileRepository` arayüzü: `list(dir): Flow<Outcome<List<FileNode>>>`, `stat`, `exists`
  - `LocalFileDataSource` — `java.io.File` tabanlı, IO dispatcher'da
  - Kabul: repository unit testleri (geçici dizin üzerinde), gizli dosya filtresi, sıralama.
  - Bağımlılık: F-0.2

- [ ] **F-1.3 — Gezgin ekranı: listeleme**
  - `BrowserRoute` + `BrowserScreen` + `BrowserViewModel` (`BrowserUiState`, `BrowserAction`)
  - Breadcrumb, liste/ızgara, ikon eşlemesi (MIME → ikon), thumbnail (Coil, görsel/video/APK)
  - Kaydırma konumu klasör başına hatırlanır (geri dönünce aynı yerde)
  - Kabul: 10.000 dosyalı klasör < 300 ms; kaydırma jank'sız (Macrobenchmark şart değil, gözle).
  - Bağımlılık: F-1.2

- [ ] **F-1.4 — Sıralama, görünüm ve tercihler**
  - DataStore ile `BrowserPrefs` kalıcılığı; üst çubukta sıralama menüsü
  - Klasörler üstte, gizli dosyalar toggle'ı
  - Kabul: uygulama yeniden başlayınca tercihler korunuyor.

- [ ] **F-1.5 — Seçim modu**
  - Uzun basma ile giriş, sayaç, tümünü seç, ters seç, geri tuşu davranışı (SPEC 3.2)
  - Seçim `SavedStateHandle`'da tutulur (proses ölümüne dayanıklı)
  - Kabul: döndürmede seçim kayboluyor mu testi (kaybolmamalı).

- [ ] **F-1.6 — Dosya işlemi motoru (çekirdek)**
  - `FileOperation`, `OperationProgress` (SPEC 5), `FileOperationEngine` (saf Kotlin, test edilebilir)
  - Kopyala/taşı/sil; ön hesap, 64 KB tampon, 200 ms throttle'lı progress, iptal
  - Çakışma çözümü: `ConflictResolution` (OVERWRITE/SKIP/KEEP_BOTH) + "hepsine uygula"
  - Kabul: unit testler — çakışma her üç modda, iptalde yarım dosya silinmesi,
    farklı birim taşımada kopyala+sil, bir dosya hata verince diğerlerinin devam etmesi.
  - Bağımlılık: F-1.2

- [ ] **F-1.7 — `FileOperationService` (foreground)**
  - Kuyruk, bildirim (ilerleme + iptal aksiyonu), `POST_NOTIFICATIONS` izni
  - `foregroundServiceType="dataSync"`, `MediaScannerConnection` tetikleme
  - UI: alt yapıştırma çubuğu + `ProgressSheet` + çakışma diyaloğu
  - Kabul: uygulama arka plandayken 2 GB kopyalama tamamlanıyor; bildirimden iptal çalışıyor.
  - Bağımlılık: F-1.6

- [ ] **F-1.8 — Tekil dosya işlemleri**
  - Yeniden adlandır, yeni klasör, yeni dosya, paylaş (`FileProvider` + `ACTION_SEND`),
    aç (`ACTION_VIEW`, uygun MIME), özellikler diyaloğu (SPEC 3.6)
  - `file_paths.xml` FileProvider yapılandırması
  - Kabul: Türkçe karakterli ad ile paylaşma ve açma çalışıyor.

- [ ] **F-1.9 — Arama**
  - `SearchUseCase`: özyinelemeli, akışlı (`Flow<FileNode>`), iptal edilebilir,
    canonical path seti ile döngü koruması
  - `SearchScreen`, 250 ms debounce, sonuçtan klasöre atlama
  - Kabul: symlink döngülü ağaçta bitiyor; sorgu değişince eski tarama iptal oluyor.

- [ ] **F-1.10 — Ana ekran**
  - Depolama kartları (`StorageManager.storageVolumes` → `StorageVolumeInfo`, `:core:storage`)
  - Kategoriler (MediaStore MIME sorguları), son değişenler
  - Kabul: SD kart takılıyken ikinci kart görünüyor.

- [ ] **F-1.11 — M1 cilası**
  - Boş durum ekranları, hata mesajları (Türkçe), `strings.xml` (tr varsayılan + `values-en`)
  - Hakkında ekranı: "Bu uygulama internete çıkmaz, reklam ve telemetri içermez" + izin listesi
  - `INTERNET` izninin manifest'te olmadığını doğrulayan test
  - Kabul: SPEC 7'deki tüm kabul kriterleri işaretli.

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
