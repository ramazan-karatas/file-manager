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
