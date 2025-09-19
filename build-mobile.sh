#!/bin/bash

# Sayı Bulma Oyunu - Mobil Uygulama Build Script
# Bu script mobil uygulamayı build eder

echo "🚀 Sayı Bulma Oyunu - Mobil Build Başlıyor..."

# Mobil uygulama klasörüne git
cd mobile-app

echo "📦 Bağımlılıkları kontrol ediyor..."
if [ ! -d "node_modules" ]; then
    echo "📥 Node modules yükleniyor..."
    npm install
fi

echo "🔧 Cordova platformları kontrol ediliyor..."

# iOS platform kontrolü
if [ ! -d "platforms/ios" ]; then
    echo "🍎 iOS platformu ekleniyor..."
    npx cordova platform add ios
else
    echo "✅ iOS platformu mevcut"
fi

# Android platform kontrolü  
if [ ! -d "platforms/android" ]; then
    echo "🤖 Android platformu ekleniyor..."
    npx cordova platform add android
else
    echo "✅ Android platformu mevcut"
fi

echo "🏗️ Build işlemi başlıyor..."

# Build seçeneği
echo "Hangi platformu build etmek istiyorsunuz?"
echo "1) iOS"
echo "2) Android" 
echo "3) Her ikisi"
echo "4) Browser test"

read -p "Seçiminiz (1-4): " choice

case $choice in
    1)
        echo "🍎 iOS build ediliyor..."
        if command -v xcodebuild &> /dev/null; then
            npx cordova build ios
            echo "✅ iOS build tamamlandı!"
            echo "📱 Simulator'da çalıştırmak için: cordova emulate ios"
        else
            echo "❌ Xcode yüklü değil. Lütfen App Store'dan Xcode'u yükleyin."
        fi
        ;;
    2)
        echo "🤖 Android build ediliyor..."
        if [ -n "$JAVA_HOME" ]; then
            npx cordova build android
            echo "✅ Android build tamamlandı!"
            echo "📱 Emulator'da çalıştırmak için: cordova emulate android"
        else
            echo "❌ JAVA_HOME ayarlanmamış. Android Studio ve Java SDK kurulumu gerekli."
        fi
        ;;
    3)
        echo "🏗️ Tüm platformlar build ediliyor..."
        npx cordova build
        echo "✅ Build işlemi tamamlandı!"
        ;;
    4)
        echo "🌐 Browser test başlatılıyor..."
        echo "Tarayıcınızda http://localhost:8000 adresini açın"
        npx cordova serve
        ;;
    *)
        echo "❌ Geçersiz seçim"
        exit 1
        ;;
esac

echo ""
echo "🎉 İşlem tamamlandı!"
echo ""
echo "📋 Faydalı komutlar:"
echo "  npm run serve          - Browser'da test"
echo "  npm run build:ios      - iOS build"
echo "  npm run build:android  - Android build"
echo "  npm run emulate:ios    - iOS simulator"
echo "  npm run emulate:android - Android emulator"
echo ""
echo "📖 Detaylı bilgi için README.md dosyasını okuyun."
