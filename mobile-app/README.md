# Sayı Bulma Oyunu - Mobil Uygulama

4 haneli gizli sayıyı bulma oyunu. Hem iOS hem Android için optimize edilmiş Cordova tabanlı mobil uygulama.

## Özellikler

- 🎮 **İki Dil Desteği**: Türkçe ve İngilizce
- 🎨 **İki Tema**: Hacker ve Modern tema seçenekleri
- 📱 **Mobil Optimize**: iOS ve Android için optimize edilmiş arayüz
- 🔥 **Haptic Feedback**: Titreşim geri bildirimi
- 💾 **Offline Çalışma**: Service Worker ile çevrimdışı destek
- 🎯 **Touch Optimized**: Dokunmatik ekranlar için optimize edilmiş kontroller

## Kurulum Gereksinimleri

### Genel
- Node.js (v14 veya üzeri)
- Cordova CLI: `npm install -g cordova`

### iOS Development
- macOS
- Xcode (App Store'dan)
- iOS Simulator
- Apple Developer hesabı (gerçek cihaz için)

### Android Development
- Java JDK 11 veya üzeri
- Android Studio
- Android SDK (API Level 24 veya üzeri)
- Android Emulator veya gerçek Android cihaz

## Kurulum Adımları

1. **Depoyu klonlayın**
   ```bash
   git clone [repository-url]
   cd mobile-app
   ```

2. **Bağımlılıkları yükleyin**
   ```bash
   npm install
   ```

3. **Platform ekleyin**
   ```bash
   # iOS için (sadece macOS'ta)
   cordova platform add ios
   
   # Android için
   cordova platform add android
   ```

4. **Plugin'leri yükleyin** (otomatik olarak yüklenir)
   - cordova-plugin-vibration
   - cordova-plugin-statusbar
   - cordova-plugin-device
   - cordova-plugin-splashscreen

## Build ve Çalıştırma

### Browser'da Test
```bash
cordova serve
# Tarayıcıda http://localhost:8000 adresini açın
```

### iOS Build
```bash
# Development build
cordova build ios

# Release build
cordova build ios --release

# Simulator'da çalıştır
cordova emulate ios

# Gerçek cihazda çalıştır (provisioning profile gerekli)
cordova run ios --device
```

### Android Build
```bash
# Development build
cordova build android

# Release build
cordova build android --release

# Emulator'da çalıştır
cordova emulate android

# Gerçek cihazda çalıştır
cordova run android --device
```

### APK Oluşturma (Android)
```bash
# Debug APK
cordova build android

# Release APK (keystore gerekli)
cordova build android --release
```

## Dosya Yapısı

```
mobile-app/
├── www/                    # Web uygulama dosyaları
│   ├── index.html         # Ana sayfa (Türkçe)
│   ├── en-index.html      # İngilizce sayfa
│   ├── css/
│   │   └── styles.css     # Mobil optimize CSS
│   ├── js/
│   │   ├── script.js      # Ana JavaScript (Türkçe)
│   │   └── script-en.js   # İngilizce JavaScript
│   ├── manifest.json      # PWA manifest
│   └── sw.js             # Service Worker
├── config.xml             # Cordova yapılandırması
├── platforms/             # Platform-specific dosyalar
├── plugins/              # Cordova plugin'leri
└── res/                  # Kaynaklar (ikonlar, splash)
```

## Özellik Detayları

### Tema Sistemi
- **Hacker Teması**: Yeşil Matrix tarzı görünüm
- **Modern Teması**: Mavi-beyaz modern arayüz
- LocalStorage ile tema tercihi kaydedilir

### Haptic Feedback
- Buton tıklamalarında hafif titreşim
- Hatalı girişlerde uyarı titreşimi
- Kazanma anında kutlama titreşimi

### Mobil Optimizasyonlar
- Touch-friendly buton boyutları (min 44px)
- iOS Safe Area desteği
- Android notch desteği
- Viewport optimizasyonu
- Keyboard handling

### PWA Özellikleri
- Offline çalışma
- Add to Home Screen
- Full-screen deneyim
- App-like davranış

## Geliştirme Notları

### Debug
```bash
# Remote debugging için
cordova run ios --device --debug
cordova run android --device --debug
```

### Log'lara Erişim
```bash
# iOS
cordova log ios

# Android
cordova log android
```

### Plugin Ekleme
```bash
cordova plugin add [plugin-name]
```

### Platform Kaldırma/Ekleme
```bash
cordova platform remove ios
cordova platform add ios
```

## Sorun Giderme

### iOS Build Sorunları
- Xcode'un yüklü ve güncel olduğundan emin olun
- Command Line Tools'u yükleyin: `xcode-select --install`
- Provisioning profile'ları kontrol edin

### Android Build Sorunları
- JAVA_HOME environment variable'ını ayarlayın
- Android SDK path'ini kontrol edin
- Gradle wrapper permissions: `chmod +x gradlew`

### Genel Sorunlar
- Node modules'u temizleyin: `rm -rf node_modules && npm install`
- Platform'u yeniden ekleyin: `cordova platform remove/add`
- Cache temizliği: `cordova clean`

## Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.

## İletişim

Hakan Özger - hakan@hakanozger.com
Proje Linki: https://hakanozger.com

---

## English Version

# Number Guessing Game - Mobile App

A 4-digit secret number guessing game. Cordova-based mobile application optimized for both iOS and Android.

### Features
- 🎮 **Dual Language**: Turkish and English support
- 🎨 **Two Themes**: Hacker and Modern theme options
- 📱 **Mobile Optimized**: Optimized interface for iOS and Android
- 🔥 **Haptic Feedback**: Vibration feedback
- 💾 **Offline Support**: Service Worker for offline functionality
- 🎯 **Touch Optimized**: Optimized controls for touch screens

### Quick Start
1. Install dependencies: `npm install`
2. Add platforms: `cordova platform add ios android`
3. Build: `cordova build`
4. Run: `cordova run ios` or `cordova run android`

For detailed instructions, see the Turkish section above.
