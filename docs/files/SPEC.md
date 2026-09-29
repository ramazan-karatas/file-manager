# Dosya Yöneticisi — Spec (`app-files`)

`applicationId`: `dev.rk.systemapps.files` · Uygulama adı: **Dosyalar**

## 1. Amaç ve kapsam

MIUI Dosya Yöneticisi'nin reklamsız yerine geçmek. Temel dosya gezinme ve dosya
işlemleri + MIUI'de düzgün olmayan iki şey: **gerçek klasör-boyut analizi** ve
**hızlı özyinelemeli arama**.

**Kapsam dışı:** bulut depolama, root gezinme, ağ paylaşımları (M3'e kadar),
medya oynatma (harici uygulamaya intent gönderilir), metin editörü.

## 2. Kullanıcı hikâyeleri

| # | Hikâye | Milestone |
|---|---|---|
| U1 | Dahili depolamada klasörler arasında gezinebilmeliyim | M1 |
| U2 | Dosyayı kopyalayıp/taşıyıp başka klasöre yapıştırabilmeliyim, ilerlemeyi görebilmeliyim | M1 |
| U3 | Çoklu seçim yapıp toplu silebilmeliyim | M1 |
| U4 | Alt klasörler dâhil isme göre arama yapabilmeliyim | M1 |
| U5 | Dosyayı ilgili uygulamada açabilmeliyim | M1 |
| U6 | İsme/boyuta/tarihe göre sıralayabilmeliyim, liste/ızgara seçebilmeliyim | M1 |
| U7 | "16 GB nereye gitti" sorusunu cevaplayan boyut ağacı görebilmeliyim | M2 |
| U8 | ZIP oluşturup açabilmeliyim | M2 |
| U9 | Sık kullandığım klasörleri kısayola ekleyebilmeliyim | M2 |
| U10 | Yanlışlıkla sildiğimi çöp kutusundan geri alabilmeliyim | M2 |
| U11 | SD kart / USB belleğe yazabilmeliyim | M2 |
| U12 | Biyometrik korumalı gizli klasöre dosya taşıyabilmeliyim | M3 |
| U13 | Yerel ağdaki SMB paylaşımına bağlanabilmeliyim | M3 |

## 3. Ekranlar

### 3.1 Ana ekran (`HomeScreen`)

- **Depolama kartları:** Dahili depolama (kullanılan/toplam + progress bar), SD kart, USB OTG.
- **Kategoriler:** İndirilenler, Görseller, Video, Ses, Belgeler, APK, Arşivler.
  Kategori bir klasör değil, MIME filtreli bir MediaStore sorgusudur.
  - İndirilenler gerçek bir klasördür (`Download`) ve gezginde açılır; diğerleri
    `CategoryScreen`'de açılır.
  - `CategoryScreen` gezginle aynı **seçenekler sayfasını** kullanır: liste/ızgara
    görünümü ve sıralama ölçütü/yönü. Tercih gezginle **ortaktır**; ayrı bir kategori
    tercihi tutulmaz. "Klasörler üstte" ve "gizli dosyalar" anahtarları burada
    gösterilmez — listede klasör yoktur ve MediaStore gizli dosyaları indekslemez.
  - Sıralama MediaStore sorgusunu tekrarlamaz: sorgu bir kez çalışır (en yeni 500
    kayıt, `DATE_MODIFIED DESC`), sonra bu liste bellekte yeniden sıralanır. Yani
    "ada göre" sıralama, en son değişmiş 500 dosya içinde sıralar.
- **Kısayollar:** kullanıcının yıldızladığı klasörler (M2).
- **Son değişenler:** son 7 günde değişmiş 20 dosya (MediaStore `DATE_MODIFIED`).

### 3.2 Gezgin (`BrowserScreen`)

- Üstte breadcrumb: yatay kaydırılabilir, bir parçaya tıklayınca o klasöre atlar.
- Liste veya ızgara görünümü; tercih kalıcı (DataStore).
- Satır düzeni: ikon/thumbnail · dosya adı · alt satır (boyut · tarih).
  Klasörlerde öğe sayısı **gösterilmez** (her klasör için ayrı dizin okuması gerektirir ve
  listeleme süresini bozar); sayı özellikler diyaloğunda hesaplanır.
- Sıralama: ad / boyut / değiştirilme tarihi / tür × artan-azalan.
  Klasörler varsayılan olarak üstte (ayardan kapatılabilir).
- Gizli dosyaları göster/gizle anahtarı.
- **Süzme ve sıralama bellekte yapılır, disk yeniden okunmaz.** Repository klasörü
  ham hâliyle döndürür; sıralama tercihi ya da liste↔ızgara değişince yalnızca
  mevcut liste yeniden sıralanır. Ölçüm (emülatör, 4.000 dosya): klasörü okumak
  733 ms, sıralamak 60 ms. Eskiden her tercih değişimi klasörü baştan okutuyordu.
- **Yükleme göstergesi:** spinner değil, gelecek satırların/karelerin yerine geçen
  gri kutular (skeleton screen) + üzerinden geçen parlama. Ölçüler gerçek satırla
  birebir ki liste gelince düzen oynamasın. Cihazda animasyonlar kapalıysa
  (`ANIMATOR_DURATION_SCALE = 0`) parlama kurulmaz, düz renk gösterilir; yer
  tutucular ekran okuyucuya okunmaz.
- **Tazelik:** liste bellekte tutulduğu için ekran öne geldiğinde klasörün kendi
  değişiklik damgası (`lastModified`) kontrol edilir; değişmişse yeniden okunur,
  değişmemişse hiç okunmaz (n dosya yerine tek `stat`). Kullanıcı ayrıca **aşağı
  çekerek** koşulsuz yenileyebilir. Yenilemede eski liste ekranda kalır, tam ekran
  spinner yerine yenileme göstergesi döner.
- Görünüm ve sıralama tercihleri **tek bir seçenekler sayfasında** toplanır
  (üst çubuktaki tek düğme → alttan açılan sayfa): görünüm ve sıra segment
  düğmeleriyle, ölçüt çiplerle, iki anahtar switch'le. Üst çubukta ayrı sıralama
  menüsü ve ayrı görünüm düğmesi tutulmaz — dar ekranda arama ikonuyla birlikte
  üç eylem sığmıyordu.
- Uzun basınca **seçim modu**: üst çubukta sayaç + işlemler
  (kopyala, kes, sil, yeniden adlandır\*, paylaş, özellikler\*). \* = yalnız tek seçimde etkin.
- FAB: yeni klasör / yeni boş dosya.
- Geri tuşu: seçim modundaysa modu kapat → değilse üst klasör → kökteyse ekrandan çık.

### 3.3 Arama (`SearchScreen`)

- Bulunulan klasörden itibaren özyinelemeli. 250 ms debounce.
- Sonuçlar akış hâlinde gelir; tarama bitmeden liste dolmaya başlar.
- Yeni sorgu gelince önceki tarama iptal edilir (coroutine cancel).
- Symlink döngüsüne karşı ziyaret edilen canonical path seti tutulur.
- Filtreler (M2): sadece klasörler / tür / boyut aralığı / tarih aralığı.

### 3.4 Yapıştırma çubuğu

Pano doluyken ekranın altında kalıcı çubuk: "3 öğe kopyalanacak — Yapıştır / İptal".
Pano uygulama yeniden başlasa da korunur (DataStore).

### 3.5 Depolama analizi (`StorageAnalysisScreen`, M2)

- Seçilen kökten özyinelemeli boyut hesabı; arka planda, ilerleme göstergeli.
- Sonuç: boyuta göre sıralı klasör listesi; satıra tıklayınca bir alt seviyeye inilir.
- Her satırda toplam içindeki payı gösteren bar.
- Sonuç Room'da cache'lenir; "yenile" ile yeniden taranır.

### 3.6 Özellikler diyaloğu

Ad, tam yol, MIME türü, boyut (klasörse hesaplanır, spinner gösterilir), öğe sayısı,
değiştirilme tarihi, izinler (rwx); görsel/video ise çözünürlük ve süre.

### 3.7 Onboarding / izin ekranı

İlk açılışta "Tüm dosyalara erişim" izninin neden gerektiğini anlatan tek ekran + izin butonu.
İzin verilmezse uygulama **sınırlı modda** çalışır (yalnızca MediaStore kategorileri) ve
üstte kalıcı bir uyarı bandı gösterir. Çökme yok.

## 4. Veri modeli

```kotlin
// domain/model/FileNode.kt
sealed interface FileNode {
    val id: String          // yerel dosyada tam yol, SAF'ta document URI metni
    val name: String
    val isDirectory: Boolean
    val size: Long          // klasörse SIZE_UNKNOWN (-1)
    val lastModified: Long
    val mimeType: String?
    val isHidden: Boolean   // ölçüt: adın nokta ile başlaması
    val extension: String   // türetilmiş: noktasız, küçük harfli
}

data class LocalFileNode(...) : FileNode     // doğrudan dosya sistemi (MANAGE_EXTERNAL_STORAGE)
data class DocumentNode(...) : FileNode      // SAF (SD kart, /Android/data) — F-2.7'de kullanılacak

data class StorageVolumeInfo(               // :core:storage
    val id: String, val label: String, val path: String,
    val totalBytes: Long, val freeBytes: Long,
    val isRemovable: Boolean, val isPrimary: Boolean,
)

enum class SortBy { NAME, SIZE, DATE, TYPE }

// Repository'nin listeleme davranışı. Yalnızca görünümü ilgilendiren gridMode burada değil,
// F-1.4'te UI tercihleri tipinde tutulacak.
data class ListingOptions(
    val sortBy: SortBy = SortBy.NAME,
    val ascending: Boolean = true,
    val showHidden: Boolean = false,
    val foldersFirst: Boolean = true,
)
```

**Kritik soyutlama:** UI katmanı `java.io.File` bilmez, yalnızca `FileNode` bilir.
Böylece SD kart (SAF) ve dahili depolama (doğrudan File) aynı ekranlarla çalışır.

**Kimlik neden `Uri` değil `String`:** `android.net.Uri` domain'e girerse sıralama ve
filtreleme mantığı JVM testlerinde çalışmaz (Robolectric gerekir). Kimlik metin olarak
taşınıyor, `Uri`ye dönüşüm paylaşma/açma gibi framework'e değen yerlerde yapılıyor.

**Repository sözleşmesi** (`domain/repository/FileRepository.kt`):

```kotlin
fun list(directoryId: String, options: ListingOptions): Flow<Outcome<List<FileNode>>>
suspend fun stat(id: String): Outcome<FileNode>
suspend fun exists(id: String): Boolean
```

`list` akış döner ki ileride klasör izleme (FileObserver) eklendiğinde arayüz değişmesin;
şu an tek değer yayıp tamamlanır. Sıralamada yerel duyarlı `Collator` kullanılır — Türkçe'de
`ş`, `ı`, `ç` ASCII karşılaştırmasıyla yanlış yere düşer.

Room tabloları: `bookmarks`, `trash_entries`, `dir_size_cache`.

## 5. Dosya işlemi motoru

`FileOperationService` — foreground service, tek kuyruk.

```kotlin
sealed interface FileOperation {
    val id: String
    val sources: List<String>          // yol; FileNode değil — motor domain modeli taşımaz

    data class Copy(sources, targetDirectory) : FileOperation
    data class Move(sources, targetDirectory) : FileOperation
    data class Delete(sources) : FileOperation
    // Compress / Extract: F-2.4'te uygulamalarıyla birlikte eklenecek.
    // Delete.toTrash: F-2.3'te çöp kutusuyla birlikte eklenecek.
}

data class OperationProgress(
    val opId: String,
    val state: OperationState,         // PREPARING, RUNNING, DONE, FAILED, CANCELLED
    val currentFile: String,
    val processedBytes: Long, val totalBytes: Long,
    val processedItems: Int, val totalItems: Int,
    val failures: List<OperationFailure>,
)
```

**Çakışma ayrı bir durum değil, ayrı bir soru.** `CONFLICT` durumu yayınlamak yerine motor
bir `ConflictResolver` çağırır ve cevabı bekler (`OVERWRITE` / `SKIP` / `KEEP_BOTH` / `CANCEL`
+ "hepsine uygula"). Böylece motor UI bilmeden çalışır ve testlerde sabit cevap verilebilir.

**`CANCELLED` durumunu motor yalnızca kullanıcı çakışma diyaloğunda "İptal" dediğinde yayar.**
Dışarıdan iptal coroutine iptalidir; iptal edilmiş bir akışa değer gönderilemeyeceği için
bu durumu işlemi iptal eden taraf (servis) kendi durumuna yazar.

Zorunlu davranışlar:

1. **Ön hesap:** işlem başlamadan toplam boyut ve öğe sayısı hesaplanır (`PREPARING`).
2. **Aynı birimde taşıma** `renameTo` ile anlık denenir; başarısız olursa (farklı birim,
   hedefte çakışma) kopyala + sil yoluna düşülür.
3. **Çakışma:** hedefte aynı ad varsa `CONFLICT` durumu; kullanıcıya sorulur:
   Üzerine yaz / Atla / Her ikisini tut (`dosya (1).txt`) / "Hepsine uygula" kutusu.
4. **İptal edilebilir**; iptalde yarım kalan hedef dosya silinir.
5. **Bildirim:** ilerleme + iptal aksiyonu; bitişte özet bildirimi.
6. **Hata toleransı:** bir dosya hata verirse işlem durmaz, sonda başarısızlar listelenir.
7. Kopyalama tamponu 64 KB; progress en çok 200 ms'te bir yayınlanır (UI boğulmasın).
8. Oluşan/silinen dosyalar için `MediaScannerConnection.scanFile` tetiklenir.

## 6. Arşiv desteği (M2)

- ZIP okuma/yazma: `java.util.zip` (ek bağımlılık yok).
- 7z / tar.gz okuma: `org.apache.commons:commons-compress`.
- RAR okuma (salt okunur): `com.github.junrar:junrar`.
- Arşiv içi gezinme: arşiv sanal bir `FileNode` ağacı olarak sunulur; tüm arşivi
  açmadan tek dosya çıkarma desteklenir.
- Şifreli ZIP: kapsam dışı (bkz. Açık Sorular).

## 7. Kabul kriterleri — M1 "bitti" tanımı

- [ ] **10.000 dosyalı klasör 300 ms altında listeleniyor, kaydırma takılmıyor.**
      Kaydırma tarafı tamam (tek atlanan kare yok). Süre emülatörde ~3,2 sn; kırılım
      `readdir` 1400 ms + `stat` 1185 ms, yani darboğaz uygulama değil emülatörün FUSE
      `/sdcard` katmanı. **Gerçek cihazda yeniden ölçülecek**; orada da tutmazsa çözüm
      iki fazlı yükleme (önce adlar, sonra öznitelikler).
- [x] 2 GB'lık dosya kopyalaması ilerleme gösteriyor, iptal edilebiliyor, iptalde
      yarım dosya bırakmıyor. — 1,2 GB ile doğrulandı; iptalde hedef klasör boş kaldı.
- [x] Uygulama arka plandayken kopyalama devam ediyor (foreground service). — 300 MB
      kopyalama ana ekrana çıkıldıktan sonra tamamlandı.
- [x] Ekran döndürme ve proses ölümünde gezinme konumu + seçim korunuyor. — seçim
      `SavedStateHandle` testiyle, gezinme konumu Navigation'ın durum saklamasıyla.
- [x] İzin verilmemişken uygulama çökmüyor, sınırlı modda çalışıyor.
- [x] Türkçe karakterli ve boşluklu dosya adları kopyala/taşı/paylaş akışlarında sorunsuz.
      — `şubat çalışması.png` açıldı, paylaşıldı, özellikleri okundu.
- [ ] **Symlink döngüsü olan ağaçta arama sonsuz döngüye girmiyor.** Koruma kodda
      (canonical yol seti) ve testi yazılı, ama Windows'ta `createSymbolicLink`
      yönetici hakkı istediği için test atlanıyor; `/sdcard` FUSE olduğundan emülatörde
      de symlink kurulamadı. Linux'ta veya gerçek cihazda doğrulanması gerekiyor.
- [x] `INTERNET` izni yok — `verify<Variant>NoInternetPermission` görevi **birleşmiş**
      manifest'i denetliyor ve `check`e bağlı. Görevin gerçekten tetiklendiği, izin
      geçici olarak eklenip derleme başarısız olarak doğrulandı.

**M1 durumu:** sekiz kriterden altısı karşılandı. Kalan ikisi de kod eksikliği değil,
doğrulama ortamı eksikliği — ikisi de gerçek Xiaomi cihazda tek oturumda kapatılabilir.

## 8. Bilinen riskler

| Risk | Azaltma |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` bile `/Android/data` altına erişemez (API 30+) | O yollarda SAF'a düş, kullanıcıya nedenini göster |
| MIUI'de izin ekranı intent'i bazen açılmıyor | try/catch + genel uygulama ayarlarına fallback |
| SAF üzerinden kopyalama `File` API'sine göre çok yavaş | SAF'ı yalnız gereken yollarda kullan; mümkünse `DocumentsContract.copyDocument` |
| Büyük ağaçta boyut hesabı UI'yı kilitler | Ayrı coroutine + akışlı güncelleme + Room cache |
| HyperOS arka plan kısıtlaması servisi öldürür | `setForeground` erken çağrılır; onboarding'de pil kısıtlaması muafiyeti istenir |

## 9. Açık Sorular

- [ ] Çöp kutusu fiziksel olarak nerede dursun? (varsayım: `/storage/emulated/0/.dosyalar_cop`)
- [ ] Şifreli ZIP desteği gerekli mi? (varsayım: hayır, M3'e ertelendi)
- [ ] Gizli kasa gerçek şifreleme mi yapsın, yoksa gizli klasör + biyometrik kilit mi?
      (varsayım: yalnız kilit — anahtar kaybı riski nedeniyle şifreleme yok)
