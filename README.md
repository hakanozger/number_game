# 🎮 Number Guessing Game / Sayı Bulma Oyunu

4 haneli gizli sayıyı bulma oyunu - Web ve mobil versiyonları ile tam kapsamlı proje.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform: Web](https://img.shields.io/badge/Platform-Web-blue)](https://github.com/hakanozger/number_game)
[![Platform: Mobile](https://img.shields.io/badge/Platform-Mobile-green)](https://github.com/hakanozger/number_game/tree/main/mobile-app)

## 🌟 Özellikler / Features

### 🎯 Oyun Özellikleri / Game Features
- **4 haneli sayı bulma** / 4-digit number guessing
- **İpuçları sistemi** / Hints system (correct digits & positions)
- **Tahmin geçmişi** / Guess history
- **İki dil desteği** / Dual language support (Turkish/English)
- **İki tema** / Two themes (Hacker/Modern)

### 💻 Web Versiyonu / Web Version
- Responsive tasarım / Responsive design
- Modern CSS3 animasyonlar / Modern CSS3 animations
- LocalStorage tema kaydetme / LocalStorage theme saving
- Font Awesome ikonlar / Font Awesome icons

### 📱 Mobil Versiyonu / Mobile Version
- **iOS ve Android desteği** / iOS & Android support
- **Cordova hybrid uygulama** / Cordova hybrid app
- **Haptic feedback** / Vibration feedback
- **PWA özellikleri** / PWA features
- **Offline çalışma** / Offline functionality
- **Touch-optimized UI** / Touch-optimized interface

## 🚀 Hızlı Başlangıç / Quick Start

### 🌐 Web Versiyonu / Web Version

Web versiyonunu doğrudan tarayıcınızda açabilirsiniz:
You can open the web version directly in your browser:

```bash
# Türkçe / Turkish
open number/index.html

# İngilizce / English  
open number/en-index.html
```

### 📱 Mobil Versiyonu / Mobile Version

Mobil uygulamayı geliştirmek için:
To develop the mobile app:

```bash
# Mobil klasöre git / Go to mobile folder
cd mobile-app

# Bağımlılıkları yükle / Install dependencies
npm install

# Browser'da test et / Test in browser
npm run serve
# http://localhost:8000 adresini aç / Open http://localhost:8000

# iOS build (macOS gerekli / macOS required)
npm run build:ios

# Android build (Java SDK gerekli / Java SDK required)
npm run build:android
```

## 📂 Proje Yapısı / Project Structure

```
number_game/
├── 🌐 number/                     # Web versiyonu / Web version
│   ├── index.html                # Ana sayfa (Türkçe) / Main page (Turkish)
│   ├── en-index.html             # İngilizce sayfa / English page
│   ├── script.js                 # JavaScript (Türkçe) / JavaScript (Turkish)
│   ├── script-en.js              # JavaScript (İngilizce) / JavaScript (English)
│   └── styles.css                # CSS stilleri / CSS styles
├── 📱 mobile-app/                 # Mobil versiyonu / Mobile version
│   ├── www/                      # Web dosyaları / Web files
│   ├── config.xml                # Cordova yapılandırması / Cordova config
│   ├── package.json              # NPM yapılandırması / NPM config
│   └── README.md                 # Mobil dokümantasyon / Mobile docs
├── build-mobile.sh               # Mobil build script / Mobile build script
├── MOBIL-UYGULAMA-OZET.md       # Mobil özet / Mobile summary
└── README.md                     # Bu dosya / This file
```

## 🎮 Nasıl Oynanır? / How to Play?

### Türkçe
1. Bilgisayar 4 haneli benzersiz rakamlardan oluşan bir sayı seçer
2. Tahmininizi girin (örn: 1234)
3. **Doğru Rakam**: Tahmininizde bulunan doğru rakamların sayısı
4. **Doğru Yer**: Hem rakam hem de yeri doğru olan rakamların sayısı
5. İpuçlarını kullanarak gizli sayıyı bulun!

### English
1. Computer selects a 4-digit number with unique digits
2. Enter your guess (e.g: 1234)
3. **Correct Digits**: Number of correct digits in your guess
4. **Correct Position**: Number of digits that are both correct and in right position
5. Use the hints to find the secret number!

## 🛠 Geliştirme / Development

### Gereksinimler / Requirements

#### Web Versiyonu / Web Version
- Modern web tarayıcısı / Modern web browser
- HTTP server (opsiyonel) / HTTP server (optional)

#### Mobil Versiyonu / Mobile Version
- **Node.js** v14+
- **Cordova CLI**: `npm install -g cordova`

**iOS için / For iOS:**
- macOS
- Xcode
- iOS Simulator

**Android için / For Android:**
- Java JDK 11+
- Android Studio
- Android SDK (API 24+)

### Kurulum / Installation

```bash
# Repoyu klonla / Clone repo
git clone https://github.com/hakanozger/number_game.git
cd number_game

# Mobil geliştirme için / For mobile development
cd mobile-app
npm install

# Platformları ekle / Add platforms
cordova platform add ios android
```

### Build Komutları / Build Commands

```bash
# Web versiyonu (HTTP server) / Web version (HTTP server)
python3 -m http.server 8000
# veya / or
npx serve number

# Mobil build / Mobile build
cd mobile-app

# Otomatik build / Automatic build
../build-mobile.sh

# Manuel build / Manual build
npm run build:ios      # iOS
npm run build:android  # Android
npm run serve          # Browser test
```

## 🎨 Tema Sistemi / Theme System

### Hacker Teması / Hacker Theme
- Siyah arkaplan / Black background (`#0a0a0a`)
- Yeşil Matrix tarzı / Green Matrix style (`#00ff00`)
- Monospace font / Monospace font (Courier New)

### Modern Teması / Modern Theme
- Açık arkaplan / Light background (`#f5f5f5`)
- Mavi vurgular / Blue accents (`#3498db`)
- Sans-serif font / Sans-serif font (Segoe UI)

## 📱 Mobil Özellikler / Mobile Features

### Touch Optimization
- Minimum 44px buton boyutları / Minimum 44px button sizes
- iOS Safe Area desteği / iOS Safe Area support
- Android Notch desteği / Android Notch support

### Haptic Feedback
- Buton tıklama titreşimi / Button tap vibration
- Hata uyarı titreşimi / Error warning vibration
- Kazanma kutlama titreşimi / Win celebration vibration

### PWA Features
- Offline çalışma / Offline functionality
- Add to Home Screen / Add to Home Screen
- Service Worker cache / Service Worker cache
- App-like deneyim / App-like experience

## 🌍 Çok Dil Desteği / Multi-language Support

| Dil / Language | Web Dosyası / Web File | Mobil Dosyası / Mobile File |
|---|---|---|
| 🇹🇷 Türkçe | `number/index.html` | `mobile-app/www/index.html` |
| 🇺🇸 English | `number/en-index.html` | `mobile-app/www/en-index.html` |

## 📊 Teknik Detaylar / Technical Details

### Web Teknolojileri / Web Technologies
- **HTML5** - Semantic markup
- **CSS3** - Flexbox, Grid, Animations
- **Vanilla JavaScript** - ES6+ features
- **LocalStorage** - Theme persistence

### Mobil Teknolojiler / Mobile Technologies
- **Apache Cordova** - Hybrid app framework
- **Service Worker** - Offline functionality
- **PWA** - Progressive Web App features
- **Haptic API** - Vibration feedback

## 🔧 Sorun Giderme / Troubleshooting

### Yaygın Sorunlar / Common Issues

#### iOS Build Sorunları / iOS Build Issues
```bash
# Xcode command line tools
xcode-select --install

# Developer directory
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

#### Android Build Sorunları / Android Build Issues
```bash
# JAVA_HOME ayarla / Set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home

# Android SDK
export ANDROID_HOME=/Users/$USER/Library/Android/sdk
```

#### Genel Sorunlar / General Issues
```bash
# Cache temizle / Clear cache
cordova clean
rm -rf node_modules && npm install

# Platform yenile / Refresh platforms
cordova platform remove ios android
cordova platform add ios android
```

## 🤝 Katkıda Bulunma / Contributing

1. Repo'yu fork edin / Fork the repo
2. Feature branch oluşturun / Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Değişikliklerinizi commit edin / Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Branch'i push edin / Push to branch (`git push origin feature/AmazingFeature`)
5. Pull Request açın / Open Pull Request

## 📝 Lisans / License

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

This project is licensed under the MIT License. See [LICENSE](LICENSE) file for details.

## 👨‍💻 Geliştirici / Developer

**Hakan Özger**
- Website: [hakanozger.com](https://hakanozger.com)
- Email: hakan@hakanozger.com
- GitHub: [@hakanozger](https://github.com/hakanozger)

## 🎯 Roadmap

### Yakın Gelecek / Near Future
- [ ] Leaderboard sistemi / Leaderboard system
- [ ] Multiplayer mod / Multiplayer mode
- [ ] Ses efektleri / Sound effects
- [ ] Animasyonlu confetti / Animated confetti

### Uzun Vadeli / Long Term
- [ ] App Store yayını / App Store release
- [ ] Google Play yayını / Google Play release
- [ ] Farklı zorluk seviyeleri / Different difficulty levels
- [ ] Başarım sistemi / Achievement system

---

## 🌟 Yıldız Verin! / Give a Star!

Bu proje işinize yaradıysa, lütfen ⭐ verin!

If you found this project helpful, please give it a ⭐!

**Happy Coding! 🎮📱**
