# Burakin Uzun Yolu - Kurulum (Türkçe)

## En kolay yöntem: GitHub Desktop + GitHub Actions

1. Bu klasörü GitHub Desktop ile bir repoya ekle.
2. `Commit to main` yap.
3. `Push origin` ile GitHub'a gönder.
4. GitHub'da `Actions` sekmesine gir.
5. `Build Android APK and AAB` workflow'unu çalıştır.
6. Build tamamlanınca `BurakinUzunYoluTR-builds` artifact'ini indir.
7. İçinden çıkan `app-debug.apk` dosyasını telefona gönder.
8. Telefonda APK'yı kur.

## Android Studio ile
1. Android Studio aç.
2. `Open` ile proje klasörünü seç.
3. Gradle senkronizasyonunun bitmesini bekle.
4. `Build > Build APK(s)` seç.
5. APK şu klasörde oluşur:
   `app/build/outputs/apk/debug/app-debug.apk`

## Telefona yükleme
- APK'yı WhatsApp, Telegram, kablo veya Drive ile telefona at.
- Dosyaya dokunup kur.
- Gerekirse `Bilinmeyen uygulama yükleme` iznini aç.

## Bu sürümde düzeltilenler
- Şeffaf / renksiz platform hissi giderildi.
- Arayüz tamamen Türkçeleştirildi.
- Bölümler daha dengeli olacak şekilde yeniden düzenlendi.
- Görsel katmanlar ve platform detayları güçlendirildi.
