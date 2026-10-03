# Dosyalar

Reklamsız, ağa çıkmayan, açık kaynak Android dosya yöneticisi.
Kotlin + Jetpack Compose (Material 3).

**İndir:** [v1.0.0 APK](https://github.com/ramazan-karatas/file-manager/releases/latest)
· Android 8.0 ve üzeri · [English](#english)

## Neler var

- Dosya gezgini: liste/ızgara görünümü, sıralama ölçütleri, gizli dosyalar
- Kopyala / taşı / sil / yeniden adlandır — arka planda, ilerleme bildirimli
- Özyinelemeli arama; sonuçlar tarama bitmeden akmaya başlar
- Kategoriler: görseller, video, ses, belgeler, APK, arşivler
- Uzun basarak çoklu seçim, paylaşma, özellikler penceresi

Reklam, analytics, crash reporting, telemetri yok.

### Bilinen sınırlar

- SD karta yazma henüz desteklenmiyor (dahili depolama sorunsuz).

## Derleme

```bash
./gradlew :app-files:assembleDebug     # APK
./gradlew testDebugUnitTest            # unit testler
```

JDK 21 gerekir; Gradle daemon'ı kendi ayarlıyor.

## Belgeler

- Mimari kararlar: [docs/00-architecture.md](docs/00-architecture.md)
- Spec + yol haritası: [docs/files/](docs/files/)
- Kodlama kuralları: [CLAUDE.md](CLAUDE.md)

## English

An ad-free, open-source Android file manager. My phone's built-in file manager
shows unskippable ads, so I wrote my own.

**Download:** [v1.0.0 APK](https://github.com/ramazan-karatas/file-manager/releases/latest)
· Android 8.0+

- File browser with list/grid views, sorting options, hidden files
- Copy / move / delete / rename, in the background with a progress notification
- Recursive search that streams results while the scan is still running
- Categories: images, video, audio, documents, APKs, archives
- Long-press multi-select, sharing, properties dialog

No ads, no subscriptions, no in-app purchases, no analytics, no telemetry.
Writing to an SD card is not supported yet; internal storage works fine.

Built with Kotlin and Jetpack Compose (Material 3). Needs JDK 21 to build:
`./gradlew :app-files:assembleDebug`

Feedback is welcome, especially from devices I cannot test on — please open an
issue and mention your phone model and Android version.

## Lisans

[Apache License 2.0](LICENSE).
