# Mimari ve Ortak Kararlar

## 1. Repo yapısı (monorepo, çok APK)

```
system_apps/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/libs.versions.toml        # tek merkezi bağımlılık kataloğu
├─ build-logic/                     # convention plugin'ler (M1 sonunda eklenir)
│   └─ convention/
│       ├─ AndroidApplicationConventionPlugin.kt
│       ├─ AndroidLibraryConventionPlugin.kt
│       └─ ComposeConventionPlugin.kt
├─ core/
│   ├─ common/      # dispatcher'lar, Outcome, formatter'lar (boyut, süre, tarih)
│   ├─ design/      # Material 3 tema, renk, tipografi, ortak Composable'lar
│   └─ storage/     # depolama birimleri, izin durumu, SAF köprüsü, MediaStore sorguları
├─ app-files/
└─ app-music/
```

**Bağımlılık yönü:** `app-*` → `core/*`. `core` modülleri birbirine sadece
`design → common`, `storage → common` şeklinde bağlanır. Ters yön yasak.

**`core/design` neden var:** İki uygulamanın da aynı "sistem uygulaması" hissini vermesi
gerekiyor. Tema, ikon boyutları, liste satırı yükseklikleri, boş durum ekranı,
onay dialog'u, çoklu seçim üst çubuğu — hepsi burada tek yerde.

**`core/storage` neden var:** Hem dosya yöneticisi hem müzik çalar MediaStore sorgusu,
depolama birimi listesi (`StorageManager.storageVolumes`) ve izin kontrolü yapıyor.
Tek tüketicisi olan şeyler buraya **girmez** (örn. ZIP açma → sadece `app-files`).

## 2. Teknoloji seçimleri ve gerekçeleri

| Karar | Seçim | Gerekçe |
|---|---|---|
| Dil | Kotlin 2.3.10 (AGP 9 ile yerleşik gelir) | Coroutines/Flow, Compose zorunluluğu |
| UI | Jetpack Compose + Material 3 | Dynamic color HyperOS'ta da çalışıyor; XML'e göre çok daha hızlı iterasyon |
| Navigasyon | Navigation-Compose, tek Activity | Basit; deep link ihtiyacı sınırlı |
| DI | Hilt | Standart, ViewModel/Service enjeksiyonu hazır |
| Veritabanı | Room | Müzik kütüphanesi cache'i ve çalma listeleri için |
| Oynatma | Media3 (ExoPlayer + MediaLibraryService) | MediaSession, bildirim, Bluetooth, Android Auto neredeyse bedava gelir |
| Görsel | Coil 3 | Compose entegrasyonu, custom Fetcher ile gömülü albüm kapağı |
| Async | kotlinx.coroutines + Flow | — |
| Statik analiz | Android Lint (detekt henüz eklenmedi) | — |
| Test | JUnit5 + Turbine + Robolectric (gerektiğinde) | — |

**Bilinçli olarak kullanılmayanlar:** RxJava, Dagger (Hilt dışı), Retrofit (ağ yok),
Firebase (hiçbir şeyi), herhangi bir analytics/ads SDK'sı.

## 3. SDK ve uyumluluk

- `minSdk = 26` (Android 8.0). Gerekçe: `MediaStore` ve `AudioFocusRequest` API'leri
  bu seviyeden itibaren tutarlı; daha eskisini desteklemenin maliyeti faydasından fazla.
- `targetSdk = 36`, `compileSdk = 36` (SDK'da kurulu platform: android-36).
- Test cihazı: Xiaomi / HyperOS (API 34+). **HyperOS'a özgü tuzaklar:**
  - Agresif arka plan kısıtlaması → foreground service + kullanıcıdan
    "Otomatik başlat" ve "Pil kısıtlaması yok" izni istenmeli (onboarding'de anlat).
  - `MANAGE_EXTERNAL_STORAGE` ekranına yönlendirme MIUI'de farklı davranabilir;
    `Intent.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` başarısız olursa
    genel ayarlar ekranına fallback yap.

## 4. Ortak mimari desen

```
Composable (state + lambda)
   ↑ StateFlow<UiState>        ↓ onAction(Action)
ViewModel
   ↓
UseCase (saf Kotlin, test edilebilir)
   ↓
Repository (interface, domain'de; impl data'da)
   ↓
DataSource (File I/O, MediaStore, Room, DocumentFile)
```

- Her ekran: `XxxRoute` (hiltViewModel alır) + `XxxScreen` (saf, preview'lı).
- Her ekranın `XxxUiState` data class'ı ve `XxxAction` sealed interface'i olur.
- Uzun süren işler (kopyalama, tarama, oynatma) **foreground Service** içinde;
  ViewModel servisin durumunu Flow olarak dinler.

## 5. İzin stratejisi

| İzin | Nerede | Not |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` | app-files | API 30+. Sideload olduğu için sorun yok. Onboarding'de neden gerektiği anlatılır. |
| `READ_EXTERNAL_STORAGE` | app-files (API ≤ 29) | eski cihaz fallback |
| `READ_MEDIA_AUDIO` | app-music | API 33+ |
| `READ_EXTERNAL_STORAGE` | app-music (API ≤ 32) | — |
| `FOREGROUND_SERVICE` + `_DATA_SYNC` | app-files | dosya işlemi servisi |
| `FOREGROUND_SERVICE` + `_MEDIA_PLAYBACK` | app-music | oynatma servisi |
| `POST_NOTIFICATIONS` | ikisi de | API 33+ |
| SAF (`ACTION_OPEN_DOCUMENT_TREE`) | app-files | `/Android/data` ve SD kart yazma için tek yol |

`INTERNET` izni **hiçbir modülde yok**. Bu bir güvenlik özelliği: uygulama ağa
çıkamıyorsa reklam/telemetri de gösteremez. Kullanıcıya bunu açıkça söyle (Hakkında ekranı).

## 6. Paketleme

- `applicationId`: `dev.rk.systemapps.files`, `dev.rk.systemapps.music`
- Ortak `versionCode`/`versionName` root `build.gradle.kts` içinde tanımlı.
- Release imzalama: yerel `keystore.properties` (repo'ya **girmez**, `.gitignore`).
- R8/ProGuard release'te açık; Room, Hilt, Media3 için kural dosyaları eklenir.

## 7. Açık Sorular

- [ ] Paket kökü `dev.rk.systemapps` kalsın mı, başka bir domain mi? (varsayım: kalsın)
- [ ] `build-logic` convention plugin'leri M1'de mi yoksa iki uygulama da ayağa kalkınca mı? (varsayım: M1 sonunda)
