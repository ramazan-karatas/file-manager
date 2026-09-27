# CLAUDE.md — system_apps

Bu repo, MIUI/HyperOS'un reklamlı sistem uygulamalarının yerine geçecek, reklamsız,
açık kaynak Android uygulamaları barındırır. Hedef cihaz: Xiaomi (HyperOS, API 34+).
Dağıtım: sideload APK (Play Store politikalarına uyma zorunluluğu yok).

## Şu anki kapsam

| Uygulama | Modül | Durum | Spec | Plan |
|---|---|---|---|---|
| Dosya Yöneticisi | `app-files` | planlandı | [docs/files/SPEC.md](docs/files/SPEC.md) | [docs/files/PLAN.md](docs/files/PLAN.md) |
| Müzik Çalar | `app-music` | planlandı | [docs/music/SPEC.md](docs/music/SPEC.md) | [docs/music/PLAN.md](docs/music/PLAN.md) |

Mimari kararlar ve modül grafiği: [docs/00-architecture.md](docs/00-architecture.md)

Sunum için MIUI ile karşılaştırmalı ölçüm: [docs/files/BENCHMARK.md](docs/files/BENCHMARK.md)
(ayrı oturumda, gerçek cihazla yapılacak)

## Agent için çalışma kuralları

1. **Önce spec'i oku.** Bir göreve başlamadan önce ilgili `SPEC.md` + `PLAN.md`
   dosyasını oku. Plan dosyasındaki görev ID'si (`F-1.3`, `M-2.1` gibi) ile çalış.
2. **Plandan sapma.** Spec'te olmayan bir özellik ekleme. Spec yanlışsa önce
   spec'i güncelle, sonra kodu yaz — ikisi aynı commit'te olsun.
3. **Görev bitince plan dosyasındaki kutuyu işaretle** (`- [ ]` → `- [x]`).
4. **Bir görev = bir commit.** Commit başlığı: `[F-1.3] kısa açıklama`.
5. **YAGNI.** `core/` modülüne bir şey taşımak için en az iki tüketicisi olmalı.
   Tek uygulama kullanıyorsa o uygulamanın içinde kalsın.
6. **Soru çıkarsa** spec'in "Açık Sorular" bölümüne yaz, varsayımla devam et,
   varsayımı orada belirt. İş durdurma.

## Teknoloji (değiştirme, gerekçeler docs/00-architecture.md içinde)

- AGP 9.1.0 + Gradle 9.3.1 + JDK 21, Gradle KTS + version catalog (`gradle/libs.versions.toml`)
- **AGP 9'da Kotlin desteği yerleşiktir.** `org.jetbrains.kotlin.android` plugin'ini
  hiçbir modüle ekleme — build hata verir. Compose için yalnızca
  `org.jetbrains.kotlin.plugin.compose` + `buildFeatures { compose = true }` uygulanır.
- Jetpack Compose + Material 3 (dynamic color), tek Activity
- Hilt (DI), Room (yerel veri), Media3 (oynatma), Coil 3 (görsel), kotlinx.coroutines
- `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`, JVM hedefi 17
- Paket kökü: `dev.rk.systemapps`
- Sürümler `gradle/libs.versions.toml` içinde; hepsi yerel Gradle cache'inde mevcut
  olacak şekilde seçildi. Yükseltirken `--offline` ile hâlâ derleniyor mu diye bak.

## Build komutları

```bash
./gradlew assembleDebug                      # tüm APK'lar
./gradlew :app-files:assembleDebug           # dosya yöneticisi APK
./gradlew testDebugUnitTest                  # tüm unit testler
./gradlew :app-files:installDebug            # bağlı cihaza kur
./gradlew lint                               # Android Lint
./gradlew :app-files:assembleRelease         # kucultulmus, imzali APK
```

**Release imzalama:** anahtar repo disinda (`C:/Users/ramaz/.android-keys/system_apps.jks`),
parolalar `keystore.properties` icinde ve bu dosya `.gitignore`da. Dosya yoksa release
imzasiz derlenir, yani temiz bir klon ve CI kirilmaz. Anahtar kaybedilirse uygulamanin
ustune yeni surum kurulamaz (once silmek gerekir, veriler gider) — yedeklenmeli.

Gradle daemon'ı JDK 21 ile çalışır (`gradle/gradle-daemon-jvm.properties`); `JAVA_HOME`
ayarlamaya gerek yok. `local.properties` makineye özeldir ve commit'lenmez.
detekt henüz kurulu değil — ekleneceği zaman version catalog'a eklenip burası güncellenecek.

## Kod standartları

- **Katmanlar:** `ui/` (Compose + ViewModel) → `domain/` (use case, saf Kotlin) →
  `data/` (repository, DataSource). Bağımlılık yönü sadece içe doğru.
- **State:** ViewModel tek bir `StateFlow<XxxUiState>` yayınlar. UI event'leri
  ViewModel'e fonksiyon çağrısı olarak gider (`onAction(Action)` tek giriş noktası).
- **Compose:** Composable'lar ViewModel almaz; state + lambda alır (preview edilebilir olsun).
  Ekran seviyesinde `XxxRoute` (VM'li) + `XxxScreen` (saf) ikilisi.
- **Kaynak metin yok.** Kullanıcıya görünen her string `strings.xml` içinde. Türkçe varsayılan
  (`values/strings.xml` = tr), İngilizce `values-en/strings.xml`.
- **Blocking I/O** asla main thread'de değil; `Dispatchers.IO` injected `CoroutineDispatcher` ile.
- **Hata yönetimi:** Repository'ler exception fırlatmaz, `Result<T>` veya sealed `Outcome` döner.
- **Test:** domain katmanındaki her use case için unit test zorunlu. UI testi sadece
  kritik akışlar için (kopyala/taşı, oynatma kontrolü).

## Yapma listesi

- Analytics, crash reporting, reklam SDK'sı, telemetri — **hiçbiri yok**. Bu reponun varlık sebebi bu.
- İnternet izni: `app-files` ve `app-music` M1/M2'de `INTERNET` iznine sahip **olmayacak**.
  (Dosya yöneticisi M3'te SMB/FTP için alacak, o zaman spec güncellenecek.)
- Üçüncü parti kütüphane eklemeden önce `docs/00-architecture.md` içindeki listeye bak;
  yoksa ekleme gerekçesini oraya yaz.
