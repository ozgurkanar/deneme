# Buraks Long Road - Kurulum

Bu proje Unity istemez. Native Android Java projesidir.
Görseller, karakter, arka planlar, UI, partiküller, müzik ve ses efektleri kod içinde üretilir.

## Bu sürümde düzeltilen ana hatalar

1. GitHub Actions daha önce şunu kurmaya çalışıyordu:

```text
platforms;android-37
```

Bazı GitHub SDK ortamlarında bu paket bulunmadığı için build düşüyordu.

2. Son logda Android resource compiler şu hatayı verdi:

```text
Invalid unicode escape sequence in string / string/app_name does not contain a valid string resource
```

Bunun nedeni `strings.xml` içindeki uygulama adında apostrof kullanılmasıydı. Android resource parser bu karakteri string escape olarak sorunlu okuyabildiği için uygulama etiketi `Buraks Long Road` yapıldı. Oyun içindeki başlık görselinde hikâye adı hâlâ korunur.

Bu sürümde build zinciri daha güvenli değerlere sabitlendi:

```text
Android Gradle Plugin: 8.13.2
Gradle: 8.13
JDK: 17
compileSdk: 36
targetSdk: 36
SDK platform: platforms;android-36
Build Tools: 35.0.0
```

## GitHub Desktop ile doğru yükleme

1. `BuraksLongRoad.zip` dosyasını bilgisayarda çıkar.
2. GitHub Desktop aç.
3. `File > Add local repository` seç.
4. Zip'ten çıkan `BuraksLongRoad` klasörünü seç.
5. GitHub Desktop “create a repository” derse kabul et.
6. Sol alttaki commit alanına şunu yaz:

```text
Fix Android resources and upload Buraks Long Road
```

7. `Commit to main` butonuna bas.
8. Üstte `Publish repository` butonuna bas.
9. GitHub sitesinde repository'yi aç.

Repository içinde dosyalar böyle görünmeli:

```text
app/
.github/
build.gradle
settings.gradle
gradle.properties
README.md
KURULUM_TR.md
```

Sadece şu görünüyorsa yanlış yüklemişsin:

```text
BuraksLongRoad.zip
```

Bu durumda ZIP'i açıp içindeki dosyaları yüklemen gerekir.

## En kolay yöntem: GitHub Actions ile APK almak

1. GitHub repository sayfasına gir.
2. **Actions** sekmesine gir.
3. Gerekirse workflow çalıştırmaya izin ver.
4. **Build Android APK and AAB** workflow'unu aç.
5. **Run workflow** butonuna bas.
6. Build bitince yeşil tik olan workflow sonucuna gir.
7. Sayfanın altındaki **Artifacts** bölümünden şunu indir:

```text
BuraksLongRoad-builds
```

8. İndirdiğin artifact ZIP'ini aç.
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
4. Üstten `Build > Build APKs` seç.
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

## Olası hatalar ve çözüm

### `Failed to find package 'platforms;android-37'`

Eski dosyayı kullanıyorsun. Bu güncel ZIP'i yükle. Güncel workflow `android-37` istemez.

### `Invalid unicode escape sequence in string` veya `string/app_name does not contain a valid string resource`

Eski dosyada `app_name` içinde apostrof vardı. Bu sürümde `app/src/main/res/values/strings.xml` düzeltildi ve workflow'a resource sanity check eklendi.

### Actions sekmesinde workflow çıkmıyor

Repository içinde bu dosyanın olduğundan emin ol:

```text
.github/workflows/build-android.yml
```

### Build kırmızı oldu

Workflow sonucuna gir, kırmızı satırı aç, hata metnini kopyala. En sık sebep dosyaları yanlış yüklemek veya ZIP'in içini değil ZIP dosyasını yüklemektir.

### APK telefonda kurulmuyor

Telefon güvenlik nedeniyle engelleyebilir. Dosya yöneticisi veya tarayıcı için “bilinmeyen uygulama yükleme” iznini aç.

## Google Play notu

Workflow release AAB de üretir ama şu an debug signing config ile imzalanır.
Google Play'e gerçek üretim yüklemesi için kendi upload key dosyanla signing config değiştirmen gerekir.

Google Play'e yüklemek için ayrıca şunlar gerekir:

- Play Console hesabı
- Uygulama adı ve açıklaması
- Ekran görüntüleri
- İçerik derecelendirme formu
- Data Safety formu
- Gizlilik politikası
- Gerçek release signing / upload key
