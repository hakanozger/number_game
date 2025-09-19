// Tema değiştirme işlevselliği
const hackerThemeBtn = document.getElementById('hacker-theme');
const modernThemeBtn = document.getElementById('modern-theme');

hackerThemeBtn.addEventListener('click', () => {
    document.body.classList.remove('modern-theme');
    hackerThemeBtn.classList.add('active');
    modernThemeBtn.classList.remove('active');
    localStorage.setItem('theme', 'hacker');
});

modernThemeBtn.addEventListener('click', () => {
    document.body.classList.add('modern-theme');
    modernThemeBtn.classList.add('active');
    hackerThemeBtn.classList.remove('active');
    localStorage.setItem('theme', 'modern');
});

// Kaydedilmiş temayı yükleme
const savedTheme = localStorage.getItem('theme');
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

// Dil değiştirme butonuna tema koruma işlevselliği ekle
const langSwitch = document.querySelector('.language-button a');
if (langSwitch) {
    langSwitch.addEventListener('click', function(e) {
        // Link href'ini al
        const targetUrl = this.getAttribute('href');
        
        // Mevcut temayı koruyalım
        let themeParam = '';
        if (document.body.classList.contains('modern-theme')) {
            themeParam = 'modern';
        } else {
            themeParam = 'hacker';
        }
        
        // URL'ye theme parametresi ekle
        const separator = targetUrl.includes('?') ? '&' : '?';
        const newUrl = `${targetUrl}${separator}theme=${themeParam}`;
        
        // Yeni URL'ye yönlendir
        e.preventDefault();
        window.location.href = newUrl;
    });
}

// Ana sayfa butonuna dil koruma işlevselliği ekle
const homeButton = document.querySelector('.home-button a');
if (homeButton) {
    homeButton.addEventListener('click', function(e) {
        // Link href'ini al
        const targetUrl = this.getAttribute('href');
        
        // Mevcut temayı koruyalım
        let themeParam = '';
        if (document.body.classList.contains('modern-theme')) {
            themeParam = 'modern';
        } else {
            themeParam = 'hacker';
        }
        
        // URL'ye theme parametresi ekle
        const separator = targetUrl.includes('?') ? '&' : '?';
        const newUrl = `${targetUrl}${separator}theme=${themeParam}`;
        
        // Yeni URL'ye yönlendir
        e.preventDefault();
        window.location.href = newUrl;
    });
}

// Sayfa yüklendiğinde URL'deki tema parametresini kontrol et ve uygula
window.addEventListener('load', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const themeParam = urlParams.get('theme');
    
    if (themeParam === 'modern') {
        document.body.classList.add('modern-theme');
        document.body.classList.remove('hacker-theme');
        document.getElementById('modern-theme').classList.add('active');
        document.getElementById('hacker-theme').classList.remove('active');
        localStorage.setItem('guessGameTheme', 'modern');
    } else if (themeParam === 'hacker') {
        document.body.classList.add('hacker-theme');
        document.body.classList.remove('modern-theme');
        document.getElementById('hacker-theme').classList.add('active');
        document.getElementById('modern-theme').classList.remove('active');
        localStorage.setItem('guessGameTheme', 'hacker');
    }
    
    // URL'den tema parametresini temizle (sayfa yenileme durumunda tekrar uygulanmasını önlemek için)
    if (themeParam) {
        const url = new URL(window.location.href);
        url.searchParams.delete('theme');
        window.history.replaceState({}, document.title, url);
    }
});

// Oyun değişkenleri
let secretNumber = '';
let attempts = 0;
const guessInput = document.getElementById('guess-input');
const guessBtn = document.getElementById('guess-btn');
const newGameBtn = document.getElementById('new-game-btn');
const statusMessage = document.getElementById('status-message');
const attemptsDisplay = document.getElementById('attempts');
const historyContainer = document.getElementById('history-container');

// Popup elementi
const winPopup = document.getElementById('win-popup');
const popupMessage = document.getElementById('popup-message');
const popupAttemptsCount = document.getElementById('popup-attempts-count');
const popupSecretNumber = document.getElementById('popup-secret-number');
const newGamePopupBtn = document.getElementById('new-game-popup-btn');

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
    
    console.log('Gizli sayı (geliştirme için):', secretNumber);
}

// Popup penceresini göster
function showWinPopup() {
    // Popup içeriğini güncelle
    popupMessage.textContent = `Tebrikler! Gizli sayıyı buldun!`;
    popupAttemptsCount.textContent = attempts;
    popupSecretNumber.textContent = secretNumber;
    
    // Popup'ı göster
    setTimeout(() => {
        winPopup.classList.add('active');
    }, 800); // Kısa bir gecikme ile göster
}

// Kullanıcı tahminini değerlendirme
function evaluateGuess(guess) {
    // Doğru rakam ve doğru yer sayısını hesapla
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
        return;
    }
    
    attempts++;
    attemptsDisplay.textContent = `Tahmin Sayısı: ${attempts}`;
    
    const result = evaluateGuess(guess);
    addToHistory(guess, result);
    
    // Kazanma durumu kontrolü
    if (result.correctPositions === 4) {
        statusMessage.textContent = `Tebrikler! ${attempts} tahminde doğru sayıyı buldun: ${secretNumber}`;
        statusMessage.classList.add('win-message');
        guessInput.disabled = true;
        guessBtn.disabled = true;
        
        // Başlığa hacker efekti ekle
        document.querySelector('.game-header h1').classList.add('hacker-effect');
        setTimeout(() => {
            document.querySelector('.game-header h1').classList.remove('hacker-effect');
        }, 3000);
        
        // Kazanma popup'ını göster
        showWinPopup();
    } else {
        statusMessage.textContent = `Tahmin: ${guess} - Devam et!`;
        guessInput.value = '';
        guessInput.focus();
    }
}

// Enter tuşu ile tahmin gönderme
guessInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !guessBtn.disabled) {
        submitGuess();
    }
});

// Oyun butonları işlevselliği
guessBtn.addEventListener('click', submitGuess);
newGameBtn.addEventListener('click', startNewGame);
newGamePopupBtn.addEventListener('click', startNewGame);

// Sayfa yüklendiğinde oyunu başlat
window.addEventListener('load', startNewGame);

// Sadece sayı girişine izin verme
guessInput.addEventListener('input', function() {
    this.value = this.value.replace(/[^0-9]/g, '');
    if (this.value.length > 4) {
        this.value = this.value.slice(0, 4);
    }
}); 