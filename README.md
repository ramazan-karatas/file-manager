# Dosyalar

Reklamsız, ağa çıkmayan, açık kaynak Android dosya yöneticisi.
Kotlin + Jetpack Compose (Material 3).

**İndir:** [v1.0.0 APK](https://github.com/ramazan-karatas/file-manager/releases/latest)
· Android 8.0 ve üzeri

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

## Lisans

[Apache License 2.0](LICENSE).
