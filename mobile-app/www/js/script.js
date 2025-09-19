// Mobil Sayı Bulma Oyunu - Türkçe
// Cordova device ready event
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    console.log('Cordova cihaz hazır');
    
    // Status bar ayarları (iOS için)
    if (window.StatusBar) {
        StatusBar.overlaysWebView(false);
        StatusBar.backgroundColorByHexString('#0a0a0a');
        StatusBar.styleLightContent();
    }
    
    // Screen orientation ayarları
    if (window.screen && window.screen.orientation) {
        // Portrait mode'u tercih et ama zorla değil
        try {
            screen.orientation.lock('portrait').catch(function(error) {
                console.log('Orientation lock desteklenmiyor:', error);
            });
        } catch (e) {
            console.log('Orientation lock hatası:', e);
        }
    }
    
    // Oyunu başlat
    initGame();
}

// Eğer Cordova yoksa (browser test için) direkt başlat
if (!window.cordova) {
    document.addEventListener('DOMContentLoaded', function() {
        initGame();
    });
}

// Tema değiştirme işlevselliği
let hackerThemeBtn, modernThemeBtn;

function initThemeSystem() {
    hackerThemeBtn = document.getElementById('hacker-theme');
    modernThemeBtn = document.getElementById('modern-theme');

    if (hackerThemeBtn && modernThemeBtn) {
        hackerThemeBtn.addEventListener('click', () => {
            document.body.classList.remove('modern-theme');
            hackerThemeBtn.classList.add('active');
            modernThemeBtn.classList.remove('active');
            localStorage.setItem('numberGameTheme', 'hacker');
            
            // Haptic feedback
            if (navigator.vibrate) {
                navigator.vibrate(50);
            }
        });

        modernThemeBtn.addEventListener('click', () => {
            document.body.classList.add('modern-theme');
            modernThemeBtn.classList.add('active');
            hackerThemeBtn.classList.remove('active');
            localStorage.setItem('numberGameTheme', 'modern');
            
            // Haptic feedback
            if (navigator.vibrate) {
                navigator.vibrate(50);
            }
        });

        // Kaydedilmiş temayı yükleme
        const savedTheme = localStorage.getItem('numberGameTheme');
        if (savedTheme === 'modern') {
            document.body.classList.add('modern-theme');
            modernThemeBtn.classList.add('active');
            hackerThemeBtn.classList.remove('active');
        } else {
            // Varsayılan olarak hacker teması
            document.body.classList.remove('modern-theme');
            hackerThemeBtn.classList.add('active');
            modernThemeBtn.classList.remove('active');
        }
    }
}

// Oyun değişkenleri
let secretNumber = '';
let attempts = 0;
let guessInput, guessBtn, newGameBtn, statusMessage, attemptsDisplay, historyContainer;
let winPopup, popupMessage, popupAttemptsCount, popupSecretNumber, newGamePopupBtn;

// DOM elementlerini al
function initDOMElements() {
    guessInput = document.getElementById('guess-input');
    guessBtn = document.getElementById('guess-btn');
    newGameBtn = document.getElementById('new-game-btn');
    statusMessage = document.getElementById('status-message');
    attemptsDisplay = document.getElementById('attempts');
    historyContainer = document.getElementById('history-container');

    // Popup elementi
    winPopup = document.getElementById('win-popup');
    popupMessage = document.getElementById('popup-message');
    popupAttemptsCount = document.getElementById('popup-attempts-count');
    popupSecretNumber = document.getElementById('popup-secret-number');
    newGamePopupBtn = document.getElementById('new-game-popup-btn');
}

// Rastgele 4 haneli sayı oluşturma
function generateSecretNumber() {
    let digits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
    let result = '';
    
    // İlk rakam 0 olmasın
    const firstDigit = Math.floor(Math.random() * 9) + 1;
    result += firstDigit;
    digits.splice(digits.indexOf(firstDigit.toString()), 1);
    
    // Kalan rakamları ekle
    for (let i = 0; i < 3; i++) {
        const randomIndex = Math.floor(Math.random() * digits.length);
        result += digits[randomIndex];
        digits.splice(randomIndex, 1);
    }
    
    return result;
}

// Oyunu başlatma
function startNewGame() {
    secretNumber = generateSecretNumber();
    attempts = 0;
    attemptsDisplay.textContent = `Tahmin Sayısı: ${attempts}`;
    statusMessage.textContent = 'Oyun başladı! 4 haneli gizli sayıyı tahmin et.';
    statusMessage.classList.remove('win-message');
    historyContainer.innerHTML = '';
    guessInput.value = '';
    guessInput.disabled = false;
    guessBtn.disabled = false;
    
    // Popup'ı kapat
    winPopup.classList.remove('active');
    
    // Focus input
    setTimeout(() => {
        guessInput.focus();
    }, 100);
    
    console.log('Gizli sayı (geliştirme için):', secretNumber);
}

// Popup penceresini göster
function showWinPopup() {
    // Haptic feedback - başarı titreşimi
    if (navigator.vibrate) {
        navigator.vibrate([200, 100, 200, 100, 200]);
    }
    
    // Popup içeriğini güncelle
    popupMessage.textContent = `Tebrikler! Gizli sayıyı buldun!`;
    popupAttemptsCount.textContent = attempts;
    popupSecretNumber.textContent = secretNumber;
    
    // Popup'ı göster
    setTimeout(() => {
        winPopup.classList.add('active');
    }, 800);
}

// Kullanıcı tahminini değerlendirme
function evaluateGuess(guess) {
    let correctDigits = 0;
    let correctPositions = 0;
    
    // Doğru yer sayısını hesapla
    for (let i = 0; i < 4; i++) {
        if (guess[i] === secretNumber[i]) {
            correctPositions++;
        }
    }
    
    // Doğru rakam sayısını hesapla
    const secretDigits = secretNumber.split('');
    const guessDigits = guess.split('');
    
    for (let i = 0; i < guessDigits.length; i++) {
        const index = secretDigits.indexOf(guessDigits[i]);
        if (index !== -1) {
            correctDigits++;
            secretDigits[index] = 'X'; // Aynı rakamı tekrar saymamak için
        }
    }
    
    return { correctDigits, correctPositions };
}

// Tahmin geçmişine ekleme
function addToHistory(guess, result) {
    const historyRow = document.createElement('div');
    historyRow.className = 'history-row';
    
    const guessItem = document.createElement('div');
    guessItem.textContent = guess;
    
    const correctDigitsItem = document.createElement('div');
    correctDigitsItem.textContent = result.correctDigits;
    
    const correctPositionsItem = document.createElement('div');
    correctPositionsItem.textContent = result.correctPositions;
    
    historyRow.appendChild(guessItem);
    historyRow.appendChild(correctDigitsItem);
    historyRow.appendChild(correctPositionsItem);
    
    historyContainer.prepend(historyRow);
}

// Tahmin gönderme işlemi
function submitGuess() {
    const guess = guessInput.value.trim();
    
    // Giriş doğrulama
    if (!/^\d{4}$/.test(guess)) {
        statusMessage.textContent = 'Lütfen 4 haneli bir sayı girin!';
        statusMessage.classList.add('shake');
        
        // Hata titreşimi
        if (navigator.vibrate) {
            navigator.vibrate(300);
        }
        
        setTimeout(() => {
            statusMessage.classList.remove('shake');
        }, 500);
        
        guessInput.focus();
        return;
    }
    
    attempts++;
    attemptsDisplay.textContent = `Tahmin Sayısı: ${attempts}`;
    
    const result = evaluateGuess(guess);
    addToHistory(guess, result);
    
    // Hafif haptic feedback
    if (navigator.vibrate) {
        navigator.vibrate(50);
    }
    
    // Kazanma durumu kontrolü
    if (result.correctPositions === 4) {
        statusMessage.textContent = `Tebrikler! ${attempts} tahminde doğru sayıyı buldun: ${secretNumber}`;
        statusMessage.classList.add('win-message');
        guessInput.disabled = true;
        guessBtn.disabled = true;
        
        // Başlığa hacker efekti ekle
        const gameTitle = document.querySelector('.game-header h1');
        if (gameTitle) {
            gameTitle.classList.add('hacker-effect');
            setTimeout(() => {
                gameTitle.classList.remove('hacker-effect');
            }, 3000);
        }
        
        // Kazanma popup'ını göster
        showWinPopup();
    } else {
        statusMessage.textContent = `Tahmin: ${guess} - Devam et!`;
        guessInput.value = '';
        guessInput.focus();
    }
}

// Event listener'ları kurma
function setupEventListeners() {
    // Enter tuşu ile tahmin gönderme
    guessInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !guessBtn.disabled) {
            e.preventDefault();
            submitGuess();
        }
    });

    // Touch/click events
    guessBtn.addEventListener('click', (e) => {
        e.preventDefault();
        submitGuess();
    });
    
    newGameBtn.addEventListener('click', (e) => {
        e.preventDefault();
        startNewGame();
    });
    
    newGamePopupBtn.addEventListener('click', (e) => {
        e.preventDefault();
        startNewGame();
    });

    // Sadece sayı girişine izin verme
    guessInput.addEventListener('input', function(e) {
        this.value = this.value.replace(/[^0-9]/g, '');
        if (this.value.length > 4) {
            this.value = this.value.slice(0, 4);
        }
    });

    // iOS keyboard handling
    guessInput.addEventListener('focus', function() {
        // iOS'ta klavye açıldığında scroll problemi için
        setTimeout(() => {
            document.body.scrollTop = 0;
            document.documentElement.scrollTop = 0;
        }, 300);
    });

    // Popup dışına tıklamayı engelle
    winPopup.addEventListener('click', function(e) {
        if (e.target === this) {
            e.preventDefault();
            return false;
        }
    });

    // Orientation change handling
    window.addEventListener('orientationchange', function() {
        setTimeout(() => {
            // Viewport yeniden hesaplanması için
            window.scrollTo(0, 0);
        }, 500);
    });
}

// Ana başlatma fonksiyonu
function initGame() {
    initDOMElements();
    initThemeSystem();
    setupEventListeners();
    startNewGame();
    
    // Mobil klavye optimizasyonu
    if ('ontouchstart' in window) {
        document.body.classList.add('touch-device');
    }
    
    console.log('Mobil sayı bulma oyunu başlatıldı');
}

// Service Worker kayıt (offline çalışma için)
if ('serviceWorker' in navigator && window.location.protocol === 'https:') {
    window.addEventListener('load', function() {
        navigator.serviceWorker.register('./sw.js')
            .then(function(registration) {
                console.log('ServiceWorker kayıtlı:', registration.scope);
            })
            .catch(function(error) {
                console.log('ServiceWorker kayıt hatası:', error);
            });
    });
}
