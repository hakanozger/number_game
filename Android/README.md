# 🎮 Sayı Bulma Oyunu - Android Native

Native Android uygulaması olarak geliştirilmiş 4 haneli sayı bulma oyunu. Kotlin ile yazılmış, Material Design 3 kullanan modern bir Android uygulaması.

## ✨ Özellikler

### 🎯 Oyun Özellikleri
- **4 haneli sayı bulma oyunu** - Benzersiz rakamlardan oluşan gizli sayı
- **İpuçları sistemi** - Doğru rakam ve doğru pozisyon sayacı
- **Tahmin geçmişi** - Tüm tahminleriniz kayıtlı
- **Animasyonlar** - Smooth geçişler ve kazanma efektleri
- **Hata kontrolü** - Geçersiz girişler için validasyon

### 🎨 Tema Sistemi
- **Hacker Teması** - Matrix tarzı yeşil-siyah görünüm
- **Modern Teması** - Material Design mavi-beyaz görünüm
- **Tema kaydetme** - SharedPreferences ile tema tercihi kalıcı

### 📱 Android Özellikleri
- **Haptic Feedback** - Buton tıklamaları, hatalar ve kazanma için titreşim
- **Material Design 3** - Modern Android tasarım dili
- **ViewBinding** - Type-safe view referansları
- **RecyclerView** - Performanslı liste yönetimi
- **Animasyonlar** - ObjectAnimator ile smooth efektler
- **Portrait Mode** - Dikey ekran kilidi
- **Edge-to-Edge** - Modern tam ekran deneyim

## 🏗️ Teknik Detaylar

### 🛠 Teknolojiler
- **Kotlin** - Ana programlama dili
- **Android SDK** - API Level 24+ (Android 7.0+)
- **Material Design 3** - UI/UX framework
- **ViewBinding** - Layout bağlama
- **SharedPreferences** - Tema kaydetme
- **RecyclerView** - Liste yönetimi
- **ObjectAnimator** - Animasyonlar
- **Vibrator Service** - Haptic feedback

### 📦 Bağımlılıklar
```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")

// UI Components
implementation("androidx.activity:activity:1.8.2")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.cardview:cardview:1.0.0")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

// Animation
implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")

// Preferences
implementation("androidx.preference:preference-ktx:1.2.1")
```

## 🚀 Kurulum ve Çalıştırma

### Gereksinimler
- **Android Studio** Hedgehog | 2023.1.1 veya üzeri
- **Android SDK** API Level 24+ (Android 7.0+)
- **Kotlin** 1.9.24
- **Gradle** 8.8.1

### Kurulum
1. **Android Studio'yu açın**
2. **"Open an existing Android Studio project"** seçin
3. **Android klasörünü seçin**
4. **Sync Project with Gradle Files** bekleyin
5. **Run** butonuna tıklayın

### Build Komutları
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

## 📂 Proje Yapısı

```
Android/
├── app/
│   ├── build.gradle.kts          # App-level Gradle config
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest
│       ├── java/com/hakanozger/numbergame/
│       │   ├── MainActivity.kt    # Ana activity
│       │   ├── HistoryAdapter.kt  # RecyclerView adapter
│       │   └── GuessResult.kt     # Data class (MainActivity içinde)
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml  # Ana layout
│           │   └── item_history.xml   # History item layout
│           ├── values/
│           │   ├── strings.xml        # String resources
│           │   ├── colors.xml         # Color resources
│           │   └── themes.xml         # Theme definitions
│           └── drawable/
│               ├── ic_terminal.xml    # Hacker theme icon
│               └── ic_brush.xml       # Modern theme icon
├── build.gradle.kts              # Project-level Gradle
├── gradle/libs.versions.toml     # Version catalog
└── README.md                     # Bu dosya
```

## 🎮 Oyun Kuralları

1. **Gizli Sayı**: Bilgisayar 4 haneli benzersiz rakamlardan oluşan bir sayı seçer
2. **Tahmin**: 4 haneli bir sayı girin
3. **İpuçları**:
   - **Doğru Rakam**: Tahmininizde bulunan doğru rakamların sayısı
   - **Doğru Yer**: Hem rakam hem de yeri doğru olan rakamların sayısı
4. **Amaç**: İpuçlarını kullanarak gizli sayıyı bulun!

## 🎨 Tema Sistemi

### Hacker Teması
```xml
<!-- Hacker Theme Colors -->
<color name="hacker_bg">#FF0A0A0A</color>
<color name="hacker_text">#FF00FF00</color>
<color name="hacker_primary">#FF00CC00</color>
<color name="hacker_secondary">#FF003300</color>
<color name="hacker_accent">#FF00FF33</color>
```

### Modern Teması
```xml
<!-- Modern Theme Colors -->
<color name="modern_bg">#FFF5F5F5</color>
<color name="modern_text">#FF333333</color>
<color name="modern_primary">#FF3498DB</color>
<color name="modern_secondary">#FFE8F4FC</color>
<color name="modern_accent">#FF2980B9</color>
```

## 📱 Android Özellikler Detayı

### Haptic Feedback
```kotlin
// Hafif titreşim (buton tıklamaları)
vibrator.vibrate(VibrationEffect.createOneShot(50, DEFAULT_AMPLITUDE))

// Hata titreşimi
vibrator.vibrate(VibrationEffect.createOneShot(300, DEFAULT_AMPLITUDE))

// Kazanma kutlaması
val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
```

### Animasyonlar
```kotlin
// Input shake animasyonu
val animator = ObjectAnimator.ofFloat(inputLayout, "translationX", 
    0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)

// Kazanma animasyonu
val scaleX = ObjectAnimator.ofFloat(gameTitle, "scaleX", 1f, 1.1f, 1f)
val scaleY = ObjectAnimator.ofFloat(gameTitle, "scaleY", 1f, 1.1f, 1f)
```

### SharedPreferences
```kotlin
// Tema kaydetme
sharedPreferences.edit()
    .putBoolean("is_hacker_theme", isHacker)
    .apply()

// Tema yükleme
val isHackerTheme = sharedPreferences.getBoolean("is_hacker_theme", true)
```

## 🔧 Geliştirme Notları

### ViewBinding Kullanımı
```kotlin
private lateinit var binding: ActivityMainBinding

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
}
```

### RecyclerView Implementation
```kotlin
class HistoryAdapter(private val historyList: MutableList<GuessResult>) : 
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    
    // ViewHolder, onCreateViewHolder, onBindViewHolder implementations
}
```

### Tema Değiştirme
```kotlin
private fun applyHackerTheme() {
    val bgColor = ContextCompat.getColor(this, R.color.hacker_bg)
    binding.main.setBackgroundColor(bgColor)
    // Diğer UI elementleri...
}
```

## 🐛 Sorun Giderme

### Build Hataları
```bash
# Clean project
./gradlew clean

# Invalidate caches
# File > Invalidate Caches and Restart

# Update dependencies
./gradlew --refresh-dependencies
```

### Emulator Sorunları
- **AVD Manager**'dan yeni emulator oluşturun
- **API Level 24+** seçin
- **Hardware acceleration** aktif olsun

### Vibration Çalışmıyor
- **AndroidManifest.xml**'de permission kontrol edin:
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

## 🚀 Build ve Release

### Debug APK
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release APK
```bash
# Keystore oluşturun
keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000

# Release build
./gradlew assembleRelease
```

### Play Store
1. **app-release.apk** dosyasını oluşturun
2. **Play Console**'da yeni release oluşturun
3. **APK'yı upload** edin
4. **Store listing** bilgilerini doldurun

## 📊 Performans

### APK Boyutu
- **Debug**: ~8MB
- **Release**: ~6MB (ProGuard ile)

### Minimum Gereksinimler
- **Android 7.0** (API 24)
- **RAM**: 1GB
- **Storage**: 50MB

### Test Edilen Cihazlar
- ✅ **Pixel 7** (Android 14)
- ✅ **Samsung Galaxy S21** (Android 13)
- ✅ **OnePlus 9** (Android 12)
- ✅ **Emulator** (API 24-34)

## 🤝 Katkıda Bulunma

1. Fork edin
2. Feature branch oluşturun (`git checkout -b feature/AmazingFeature`)
3. Commit edin (`git commit -m 'Add AmazingFeature'`)
4. Push edin (`git push origin feature/AmazingFeature`)
5. Pull Request açın

## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](../LICENSE) dosyasına bakın.

## 👨‍💻 Geliştirici

**Hakan Özger**
- Website: [hakanozger.com](https://hakanozger.com)
- Email: hakan@hakanozger.com
- GitHub: [@hakanozger](https://github.com/hakanozger)

---

**Happy Coding! 🎮📱**
