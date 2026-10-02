# Mimari ve Ortak Kararlar

## 1. Repo yapısı

```
system_apps/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/libs.versions.toml        # tek merkezi bağımlılık kataloğu
├─ core/
│   ├─ common/      # dispatcher'lar, Outcome, formatter'lar (boyut, süre, tarih)
│   ├─ design/      # Material 3 tema, renk, tipografi, ortak Composable'lar
│   └─ storage/     # depolama birimleri, izin durumu, SAF köprüsü, MediaStore sorguları
└─ app-files/
```

**Bağımlılık yönü:** `app-files` → `core/*`. `core` modülleri birbirine sadece
`design → common`, `storage → common` şeklinde bağlanır. Ters yön yasak.

**`core/*` neden ayrı:** Tema ve ortak Composable'lar (`design`), platform depolama
API'leri (`storage`) ve saf yardımcılar (`common`) uygulama ekranlarından ayrı duruyor;
bu sınır sayesinde `core` modülleri Android bağımlılığı olmadan ya da UI'dan bağımsız
test edilebiliyor. Tek tüketicisi olan ve uygulamaya özgü şeyler buraya **girmez**
(örn. ZIP açma → sadece `app-files`).

## 2. Teknoloji seçimleri ve gerekçeleri

| Karar | Seçim | Gerekçe |
|---|---|---|
| Dil | Kotlin 2.3.10 (AGP 9 ile yerleşik gelir) | Coroutines/Flow, Compose zorunluluğu |
| UI | Jetpack Compose + Material 3 | Dynamic color desteği; XML'e göre çok daha hızlı iterasyon |
| Navigasyon | Navigation-Compose, tek Activity | Basit; deep link ihtiyacı sınırlı |
| DI | Hilt | Standart, ViewModel/Service enjeksiyonu hazır |
| Veritabanı | Room (M2) | Depolama analizi sonuçlarının cache'i için |
| Görsel | Coil 3 | Compose entegrasyonu, custom Fetcher ile APK ikonu çıkarma |
| Async | kotlinx.coroutines + Flow | — |
| Statik analiz | Android Lint (detekt henüz eklenmedi) | — |
| Test | JUnit4 + Turbine + coroutines-test | — |

**Bilinçli olarak kullanılmayanlar:** RxJava, Dagger (Hilt dışı), Retrofit (ağ yok),
Firebase (hiçbir şeyi), herhangi bir analytics/ads SDK'sı.

## 3. SDK ve uyumluluk

- `minSdk = 26` (Android 8.0). Gerekçe: `MediaStore` API'leri bu seviyeden itibaren
  tutarlı; daha eskisini desteklemenin maliyeti faydasından fazla.
- `targetSdk = 36`, `compileSdk = 36` (SDK'da kurulu platform: android-36).
- **Üretici ROM'larında dikkat edilenler** (agresif güç yönetimi yapan cihazlar):
  - Arka plan kısıtlaması uzun işlemleri kesebilir → foreground service kullanılıyor.
  - `MANAGE_EXTERNAL_STORAGE` ekranına yönlendirme her ROM'da aynı davranmıyor;
    `Intent.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` başarısız olursa
    genel ayarlar ekranına fallback yapılıyor.

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
- Uzun süren işler (kopyalama, tarama) **foreground Service** içinde;
  ViewModel servisin durumunu Flow olarak dinler.

## 5. İzin stratejisi

| İzin | Nerede | Not |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` | app-files | API 30+. Sideload olduğu için sorun yok. Onboarding'de neden gerektiği anlatılır. |
| `READ_EXTERNAL_STORAGE` | app-files (API ≤ 29) | eski cihaz fallback |
| `READ_MEDIA_IMAGES` / `_VIDEO` / `_AUDIO` | app-files (sınırlı mod) | API 33+ |
| `READ_MEDIA_VISUAL_USER_SELECTED` | app-files | API 34+. İstenmezse sistem "yalnızca seçilenler" seçeneğini hiç sunmaz; kısmi erişim de sınırlı mod sayılır. |
| `FOREGROUND_SERVICE` + `_DATA_SYNC` | app-files | dosya işlemi servisi |
| `POST_NOTIFICATIONS` | app-files | API 33+ |
| `REQUEST_INSTALL_PACKAGES` | app-files | APK'ye dokununca sistem yükleyicisini açmak için |
| SAF (`ACTION_OPEN_DOCUMENT_TREE`) | app-files | `/Android/data` ve SD kart yazma için tek yol (M2) |

`INTERNET` izni **yok**. Bu bir güvenlik özelliği: uygulama ağa
çıkamıyorsa reklam/telemetri de gösteremez. Kullanıcıya bunu açıkça söyle (Hakkında ekranı).

## 6. Paketleme

- `applicationId`: `dev.rk.systemapps.files`
- `versionCode`/`versionName` `app-files/build.gradle.kts` içinde tanımlı.
- Release imzalama: yerel `keystore.properties` (repo'ya **girmez**, `.gitignore`).
- R8/ProGuard release'te açık; Hilt ve Room için kural dosyaları eklenir.

## 7. Açık Sorular

- [ ] Paket kökü `dev.rk.systemapps` kalsın mı, başka bir domain mi? (varsayım: kalsın)
