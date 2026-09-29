# Naz Tube Android MVP

Telefon ve tabletlerde calisan, sahibinin kendi sunucusuna yukledigi videolari oynatan Android uygulamasi ve basit yonetim paneli. Arayuz ozgun bir video kutuphanesi deneyimi sunar; YouTube markasini, kaynaklarini veya indirme mekanizmalarini kullanmaz.

## Klasorler

- `android-app`: Android Studio ile acilan Kotlin uygulamasi. Android 8.0 ve sonrasi telefon/tabletler icindir.
- `server`: Node.js ile calisan katalog, medya ve Naz Tube yonetim paneli sunucusu.

## Hizli kurulum

1. Sunucunuzda Node.js 20 veya sonrasi kurulu olsun.
2. `server` klasorunde `.env.example` dosyasini `.env` olarak kopyalayin; `ADMIN_PASSWORD`, `JWT_SECRET` ve `ENROLLMENT_CODE` degerlerini degistirin.
3. `npm install` ve `npm start` komutlarini calistirin. Uretimde bunu HTTPS saglayan Nginx veya Caddy arkasina koyun.
4. Tarayicidan `https://alanadiniz/admin` adresini acin. Giris yapip video yukleyin ve cihaz politikasini ayarlayin.
5. Android Studio ile `android-app` klasorunu acin. Uygulama ilk acilista sunucu URL'sini ve kayit kodunu sorar.

## GitHub Actions ile APK derleme

Bu klasorun **icerigini** GitHub deposunun kokune yukleyin; `.github/workflows/android-build.yml`, `android-app` ve `server` ayni seviyede olmali. Her gonderimde `Naz Tube Android Build` otomatik calisir. Basarili calismada Actions sayfasindaki `Artifacts` bolumunden `Naz-Tube-debug-apk` dosyasini indirebilirsiniz.

Bu ilk APK imzasiz debug paketidir ve Android cihazda test icindir. Play Store yayini veya kalici dagitimdan once keystore olusturup GitHub Secrets ile imzali release APK/AAB eklenmelidir.

## Uzaktan yonetim

Naz Tube panelinden her cihaz icin sunlari ayarlayabilirsiniz: uygulamayi kapatma, oynatmayi kapatma, uzaktan kilitleme, gunluk izleme limiti, ekranda gosterilecek mesaj ve engelli video listesi. Uygulama acilista ve Android'in planli senkronizasyonuyla politikayi yeniler.

Normal bir Android uygulamasi Home tusunu, uygulama kaldirmayi veya cihaz ayarlarini engelleyemez. Bunlarin da engellenmesi gerekiyorsa, kurumsal sahipli cihazlari Android Enterprise Device Owner/MDM ile kiosk modunda yonetmek gerekir.

## Guvenlik notlari

Sadece yeniden barindirma ve oynatma hakkiniz bulunan videolari yukleyin. Sunucuyu HTTPS ile yayinlayin. Kayit kodunu ve yonetici parolasini kimseyle paylasmayin. Bu basit MVP JSON dosyasi ile veri tutar; coklu yonetici veya yuksek trafik oncesinde PostgreSQL, yedekleme, oran sinirlama ve denetim kaydi eklenmelidir.
