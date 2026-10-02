# system_apps

Xiaomi HyperOS'un reklamlı sistem uygulamalarının reklamsız, ağa çıkmayan,
açık kaynak alternatifleri. Kotlin + Jetpack Compose monorepo.

| Uygulama | Modül | Ne yapıyor |
|---|---|---|
| Dosyalar | `app-files` | Dosya yöneticisi, arşiv, depolama analizi |
| Müzik | `app-music` | Yerel müzik çalar (Media3), klasör bazlı kütüphane |

- Mimari: [docs/00-architecture.md](docs/00-architecture.md)
- Agent kılavuzu / kodlama kuralları: [CLAUDE.md](CLAUDE.md)
- Spec + planlar: `docs/files/`, `docs/music/`

**Bu uygulamalarda yok:** reklam, analytics, crash reporting, telemetri, internet izni.

## Lisans

[Apache License 2.0](LICENSE). Kullanabilir, değiştirebilir, dağıtabilirsin;
lisans metnini ve değişiklik bildirimini koruman yeterli. Lisans ayrıca açık bir
patent hakkı verir — bu yazılımla ilgili patent davası açan taraf bu hakkı kaybeder.
