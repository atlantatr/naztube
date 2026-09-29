# Android mimarisi

```text
Naz Tube yonetim paneli -> Naz Tube sunucusu -> Android telefon/tablet
     |                |                      |
 video yukleme    katalog + politika       Media3 oynatici
 cihaz ayari      yetkili medya akisi      periyodik senkron
```

Uygulama ilk kurulumda sunucu adresi ve bir kayit kodu alir. Sunucu cihaza rastgele bir erisim belirteci verir; katalog, medya ve politika istekleri bu belirtecle yapilir. Panel girisi ayri bir yonetici JWT'siyle korunur.

## API

- `POST /api/devices/register`: cihaz kaydi
- `GET /api/device/config`: cihaz politikasi
- `GET /api/catalog`: cihaz icin video katalogu
- `GET /api/media/:id`: yetkili video akisi, HTTP Range destekli
- `POST /api/admin/login`: panel girisi
- `POST /api/admin/videos`: yasal video yukleme
- `PATCH /api/admin/devices/:id/policy`: uzaktan politika guncelleme

## Politika alanlari

`enabled`, `playbackEnabled`, `forceLock`, `maxDailyMinutes`, `message` ve `blockedVideoIds` uygulama tarafinda denetlenir. Politika degisiklikleri uygulama acikken sonraki yenilemede, kapaliyken WorkManager tarafindan en erken 15 dakika sonraki planli calismada gorulur. Bu aralik Android tarafindan kesin zaman garantisi vermez.

## Uretim tavsiyesi

MVP sonrasinda medya depolamayi nesne depolamaya, JSON dosyasini PostgreSQL'e ve arka plan bildirimini Firebase Cloud Messaging'e tasiyin. Cihazdan kacisi engellemeniz gerekiyorsa Device Owner/MDM kullanin; uygulama ici PIN tek basina sistem genelinde kiosk saglamaz.
