# Burak'in Uzun Yolu - LibGDX TR

Bu proje, onceki native Android Canvas prototipinden farkli olarak **LibGDX tabanli** daha profesyonel bir temel kullanir.

## Neler var?
- LibGDX `core + android + desktop` mimarisi
- Gercek PNG sprite/tileset/background assetleri
- Gercek WAV ses efektleri ve muzik dosyalari
- `tools/generate_assets.py` ile tekrar uretilebilir asset pipeline
- Turkce hikaye ve arayuz metinleri
- Oynanabilir 5 bolumlu 2D platformer prototipi
- Ana yol icin daha guvenli ziplama mesafeleri
- GitHub Actions ile APK/AAB build

## Assetler
Tum assetler proje icinde bulunur:

```text
android/assets/images/
android/assets/audio/
```

Assetleri yeniden uretmek icin:

```bash
python tools/generate_assets.py
```

## GitHub Actions ile APK alma
1. Projeyi GitHub'a yukle.
2. Actions sekmesine gir.
3. `Build LibGDX Android APK and AAB` workflow'unu calistir.
4. `BurakLibGDXTR-builds` artifact'ini indir.
5. Icindeki `android-debug.apk` veya benzer debug APK dosyasini telefona kur.

## Desktop test
Android SDK kurmadan masaustunde test etmek icin:

```bash
gradle :desktop:run
```

## Android build

```bash
gradle :android:assembleDebug
```

APK yolu:

```text
android/build/outputs/apk/debug/android-debug.apk
```

## Not
Default LibGDX fontu Turkce ozel karakterleri her ortamda garanti etmedigi icin oyun ici metinlerde ASCII-Turkce kullanildi. Proje yapisi bunu daha sonra kendi bitmap font assetimizle degistirmeye uygundur.
