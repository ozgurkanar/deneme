# Burak's Long Road - Kurulum

Bu proje Unity istemez. Native Android Java projesidir.
Görseller, karakter, arka planlar, UI, partiküller, müzik ve ses efektleri kod içinde üretilir.

## En kolay yöntem: GitHub Actions ile APK almak

1. GitHub'da yeni boş repository oluştur.
2. Bu klasördeki tüm dosyaları repository içine yükle.
3. Repository sayfasında **Actions** sekmesine gir.
4. **Build Android APK and AAB** workflow'unu aç.
5. **Run workflow** butonuna bas.
6. Build bitince workflow sonucuna gir.
7. **BuraksLongRoad-builds** artifact dosyasını indir.
8. ZIP'i aç.
9. İçinden çıkan `app-debug.apk` dosyasını Android telefona gönder.
10. Telefonda APK'yı açıp kur.

Android izin isterse:
- Ayarlar > Bilinmeyen uygulama yükleme izni
- APK'yı açtığın dosya yöneticisine veya tarayıcıya izin ver
- Tekrar APK'yı aç ve kur

## Android Studio ile kurmak

1. Android Studio kuruluysa projeyi aç.
2. `BuraksLongRoad` klasörünü seç.
3. Gradle sync bitmesini bekle.
4. Üstten Run'a bas veya Build > Build APKs seç.
5. APK konumu:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Telefona ADB ile kurmak

```bash
adb install -r app-debug.apk
```

## Kontroller

Telefon:
- Sol alt: sağ / sol hareket
- Sağ alt yukarı ok: zıplama
- Yıldız/aksiyon butonu: köprü switchleri
- Sağ üst: pause

Klavye / emulator:
- A / D veya ok tuşları: hareket
- Space / W / yukarı ok: zıplama
- E / Enter: etkileşim
- P: pause

## Google Play notu

Workflow release AAB de üretir ama şu an debug signing config ile imzalanır.
Google Play'e gerçek üretim yüklemesi için kendi upload key dosyanla signing config değiştirmen gerekir.
