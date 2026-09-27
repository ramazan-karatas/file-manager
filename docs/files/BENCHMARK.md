# Sunum için karşılaştırmalı ölçüm — görev tanımı

Bu belge ayrı bir oturumda çalışacak coding agent içindir. Önce
[CLAUDE.md](../../CLAUDE.md) ve [PLAN.md](PLAN.md) okunmalı; buradaki iş onların
kurallarına uyar (bir görev = bir commit, spec değişirse aynı commit'te güncellenir).

## Amaç

Proje sunumunda `app-files` ("Dosyalar") uygulamasını **MIUI'nin kendi dosya
yöneticisiyle** kıyaslamak. Çıktı: savunulabilir, doğrulanabilir sayılar — tahmin değil.

## Bağlam

- `app-files` M1 tamamlandı: gezinme, sıralama, seçim, kopyala/taşı/sil (foreground
  service), açma, paylaşma, yeniden adlandırma, özellikler, arama, ana ekran.
  115 birim testi, lint temiz.
- Uygulamanın `INTERNET` izni **yok**; bunu `verify<Variant>NoInternetPermission`
  Gradle görevi birleşmiş manifest üzerinden zorunlu kılıyor (`check`e bağlı).
- Kullanıcı gerçek bir Xiaomi cihaz kullanıyor. **Agent cihaza erişemez** — ölçümleri
  kullanıcı çalıştıracak. Bu yüzden her adım için kullanıcıya verilecek komut
  hazırlanmalı ve çıktının nasıl yorumlanacağı yazılmalı.
- **Cihazda depolama dolu.** Sentetik test dosyası oluşturulmayacak.

## Verilmiş kapsam kararları — yeniden tartışma

1. **Yalnızca açılış süresi otomatik ölçülecek.** `StartupTimingMetric` başka
   paketler için de çalışır, uygulamanın kaynak koduna erişim gerekmez.
2. **MIUI tarafında kaydırma otomasyonu yok.** Teknik olarak mümkün (UiAutomator ile
   MIUI arayüzünü sürmek) ama seçiciler sürüme ve dile göre kırılıyor, ayrıca iki
   uygulamanın aynı klasörde aynı mesafeyi aynı hızda kaydırdığını garanti etmek zor.
   Kaydırma karşılaştırması sunumda **yan yana ekran kaydıyla** gösterilecek.
3. **Sentetik dosya oluşturulmayacak.** Ölçüm cihazda zaten var olan kalabalık bir
   klasörle yapılacak (`DCIM/Camera`, `WhatsApp/Media` gibi). Karşılığında sonuçlar
   cihazlar arası karşılaştırılabilir olmaz; burada amaç o değil.

## Yapılacak işler

### B1 — `:benchmark` modülü

- `com.android.test` eklentisiyle yeni modül, `settings.gradle.kts`'e eklenir
- `androidx.benchmark:benchmark-macro-junit4` (sürüm version catalog'a)
- `app-files` içine `benchmark` build type: `initWith(release)`, debug anahtarıyla
  imzalı, `isProfileable = true`, `matchingFallbacks = listOf("release")`
- Uygulama kodunda değişiklik gerekmiyor; trace işaretlerine bu aşamada gerek yok

### B2 — İki paket için açılış ölçümü

- Ölçülecek paket **parametre** olur; aynı test sınıfı hem `dev.rk.systemapps.files`
  hem MIUI paketi için çalışsın
- MIUI paket adı cihaza göre değişiyor (`com.mi.android.globalFileexplorer` veya
  `com.android.fileexplorer`). Sabit yazma; kullanıcının `pm list packages` çıktısıyla
  doldurabileceği tek bir yerde tut.
- `StartupMode.COLD`, en az 10 iterasyon
- `CompilationMode.None()` ve `CompilationMode.Full()` ayrı ayrı raporlanır
- Çıktı: medyan + P90. Tek sayı değil, dağılım verilir.

### B3 — İzin ve ağ trafiği karşılaştırması

Ölçüm altyapısı gerektirmeyen, `adb` ile çıkan tablo. Kullanıcının çalıştırıp
çıktısını yapıştıracağı tek bir betik yazılmalı (`scripts/compare.sh` gibi):

- İki paketin istediği izinler (`dumpsys package <paket>`)
- İki paketin APK boyutu (`pm path` + `ls -l`)
- MIUI paketinin ağ trafiği (`dumpsys netstats detail`)
- Bizim paketin `INTERNET` iznine sahip olmadığının kanıtı

Çıktı doğrudan sunuma konabilecek bir markdown tablosuna dönüştürülmeli.

### B4 — (opsiyonel) Kendi uygulamamızda kaydırma ölçümü

Yalnızca `app-files` için `FrameTimingMetric`. "10.000 satırda düşen kare yok"
iddiasını sayıyla desteklemek için. MIUI tarafı için **yapılmayacak** (bkz. karar 2).

## Aynı oturumda kapatılabilecek iki açık kriter

[SPEC.md §7](SPEC.md) içinde iki kriter doğrulama ortamı eksikliğinden açık kaldı:

1. **10.000 dosyalı klasör 300 ms altında listeleniyor.** Emülatörde ~3,2 sn ölçüldü
   ama kırılım `readdir` 1400 ms + `stat` 1185 ms, yani darboğaz emülatörün FUSE
   `/sdcard` katmanıydı. Gerçek cihazda, **var olan kalabalık bir klasörle** yeniden
   ölçülmeli. Tutmazsa çözüm iki fazlı yükleme: önce dosya adları listelenir, boyut ve
   tarih arkadan doldurulur.
2. **Symlink döngüsünde arama sonsuza girmiyor.** Testi yazılı
   (`FileSearchTest.symlink dongusu aramayi sonsuza sokmaz`) ama Windows'ta
   `Files.createSymbolicLink` yönetici hakkı istediği için atlanıyor.
   **Cihazda da çalıştırılamaz** — `/sdcard` FUSE ve symlink desteklemiyor.
   Çözüm: testi Linux'ta çalıştırmak (WSL veya CI). Cihaz oturumunda uğraşma.

## Kabul kriterleri

- [ ] `./gradlew :benchmark:connectedBenchmarkAndroidTest` gerçek cihazda çalışıyor
- [ ] Çıktıda iki paketin soğuk açılış süresi medyan + P90 olarak var
- [ ] İzin/boyut/trafik tablosu tek komutla üretilebiliyor
- [ ] Sunuma konacak özet tablo `docs/files/BENCHMARK-SONUC.md` olarak yazıldı
- [ ] SPEC §7'deki 10.000 dosya kriteri gerçek cihaz ölçümüyle güncellendi

## Yapma listesi

- Sentetik dosya oluşturma (depolama dolu)
- MIUI arayüzünü UiAutomator ile sürme
- Emülatörde ölçüm yapıp sonucu rapora yazma — emülatör bu projede güvenilmez çıktı
  (bkz. PLAN.md F-1.3 ve F-1.9 notları)
- Ölçümü agent'ın çalıştırdığını varsayma; cihaz kullanıcıda
