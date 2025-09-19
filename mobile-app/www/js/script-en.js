// Mobile Number Guessing Game - English
// Cordova device ready event
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    console.log('Cordova device ready');
    
    // Status bar settings (for iOS)
    if (window.StatusBar) {
        StatusBar.overlaysWebView(false);
        StatusBar.backgroundColorByHexString('#0a0a0a');
        StatusBar.styleLightContent();
    }
    
    // Screen orientation settings
    if (window.screen && window.screen.orientation) {
        // Prefer portrait mode but don't force it
        try {
            screen.orientation.lock('portrait').catch(function(error) {
                console.log('Orientation lock not supported:', error);
            });
        } catch (e) {
            console.log('Orientation lock error:', e);
        }
    }
    
    // Start the game
    initGame();
}

// If Cordova is not available (for browser testing) start directly
if (!window.cordova) {
    document.addEventListener('DOMContentLoaded', function() {
        initGame();
    });
}

// Theme switching functionality
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

        // Load saved theme
        const savedTheme = localStorage.getItem('numberGameTheme');
        if (savedTheme === 'modern') {
            document.body.classList.add('modern-theme');
            modernThemeBtn.classList.add('active');
            hackerThemeBtn.classList.remove('active');
        } else {
            // Default to hacker theme
            document.body.classList.remove('modern-theme');
            hackerThemeBtn.classList.add('active');
            modernThemeBtn.classList.remove('active');
        }
    }
}

// Game variables
let secretNumber = '';
let attempts = 0;
let guessInput, guessBtn, newGameBtn, statusMessage, attemptsDisplay, historyContainer;
let winPopup, popupMessage, popupAttemptsCount, popupSecretNumber, newGamePopupBtn;

// Get DOM elements
function initDOMElements() {
    guessInput = document.getElementById('guess-input');
    guessBtn = document.getElementById('guess-btn');
    newGameBtn = document.getElementById('new-game-btn');
    statusMessage = document.getElementById('status-message');
    attemptsDisplay = document.getElementById('attempts');
    historyContainer = document.getElementById('history-container');

    // Popup elements
    winPopup = document.getElementById('win-popup');
    popupMessage = document.getElementById('popup-message');
    popupAttemptsCount = document.getElementById('popup-attempts-count');
    popupSecretNumber = document.getElementById('popup-secret-number');
    newGamePopupBtn = document.getElementById('new-game-popup-btn');
}

// Generate random 4-digit number
function generateSecretNumber() {
    let digits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
    let result = '';
    
    // First digit should not be 0
    const firstDigit = Math.floor(Math.random() * 9) + 1;
    result += firstDigit;
    digits.splice(digits.indexOf(firstDigit.toString()), 1);
    
    // Add remaining digits
    for (let i = 0; i < 3; i++) {
        const randomIndex = Math.floor(Math.random() * digits.length);
        result += digits[randomIndex];
        digits.splice(randomIndex, 1);
    }
    
    return result;
}

// Start new game
function startNewGame() {
    secretNumber = generateSecretNumber();
    attempts = 0;
    attemptsDisplay.textContent = `Attempts: ${attempts}`;
    statusMessage.textContent = 'Game started! Guess the 4-digit secret number.';
    statusMessage.classList.remove('win-message');
    historyContainer.innerHTML = '';
    guessInput.value = '';
    guessInput.disabled = false;
    guessBtn.disabled = false;
    
    // Close popup
    winPopup.classList.remove('active');
    
    // Focus input
    setTimeout(() => {
        guessInput.focus();
    }, 100);
    
    console.log('Secret number (for development):', secretNumber);
}

// Show win popup
function showWinPopup() {
    // Haptic feedback - success vibration
    if (navigator.vibrate) {
        navigator.vibrate([200, 100, 200, 100, 200]);
    }
    
    // Update popup content
    popupMessage.textContent = `Congratulations! You found the secret number!`;
    popupAttemptsCount.textContent = attempts;
    popupSecretNumber.textContent = secretNumber;
    
    // Show popup
    setTimeout(() => {
        winPopup.classList.add('active');
    }, 800);
}

// Evaluate user guess
function evaluateGuess(guess) {
    let correctDigits = 0;
    let correctPositions = 0;
    
    // Calculate correct positions
    for (let i = 0; i < 4; i++) {
        if (guess[i] === secretNumber[i]) {
            correctPositions++;
        }
    }
    
    // Calculate correct digits
    const secretDigits = secretNumber.split('');
    const guessDigits = guess.split('');
    
    for (let i = 0; i < guessDigits.length; i++) {
        const index = secretDigits.indexOf(guessDigits[i]);
        if (index !== -1) {
            correctDigits++;
            secretDigits[index] = 'X'; // Mark as used to avoid counting twice
        }
    }
    
    return { correctDigits, correctPositions };
}

// Add to guess history
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

// Submit guess
function submitGuess() {
    const guess = guessInput.value.trim();
    
    // Input validation
    if (!/^\d{4}$/.test(guess)) {
        statusMessage.textContent = 'Please enter a 4-digit number!';
        statusMessage.classList.add('shake');
        
        // Error vibration
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
    attemptsDisplay.textContent = `Attempts: ${attempts}`;
    
    const result = evaluateGuess(guess);
    addToHistory(guess, result);
    
    // Light haptic feedback
    if (navigator.vibrate) {
        navigator.vibrate(50);
    }
    
    // Check win condition
    if (result.correctPositions === 4) {
        statusMessage.textContent = `Congratulations! You found the correct number in ${attempts} attempts: ${secretNumber}`;
        statusMessage.classList.add('win-message');
        guessInput.disabled = true;
        guessBtn.disabled = true;
        
        // Add hacker effect to title
        const gameTitle = document.querySelector('.game-header h1');
        if (gameTitle) {
            gameTitle.classList.add('hacker-effect');
            setTimeout(() => {
                gameTitle.classList.remove('hacker-effect');
            }, 3000);
        }
        
        // Show win popup
        showWinPopup();
    } else {
        statusMessage.textContent = `Guess: ${guess} - Keep trying!`;
        guessInput.value = '';
        guessInput.focus();
    }
}

// Setup event listeners
function setupEventListeners() {
    // Enter key to submit guess
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

    // Allow only number input
    guessInput.addEventListener('input', function(e) {
        this.value = this.value.replace(/[^0-9]/g, '');
        if (this.value.length > 4) {
            this.value = this.value.slice(0, 4);
        }
    });

    // iOS keyboard handling
    guessInput.addEventListener('focus', function() {
        // Fix scroll issue when keyboard opens on iOS
        setTimeout(() => {
            document.body.scrollTop = 0;
            document.documentElement.scrollTop = 0;
        }, 300);
    });

    // Prevent clicking outside popup
    winPopup.addEventListener('click', function(e) {
        if (e.target === this) {
            e.preventDefault();
            return false;
        }
    });

    // Orientation change handling
    window.addEventListener('orientationchange', function() {
        setTimeout(() => {
            // Recalculate viewport
            window.scrollTo(0, 0);
        }, 500);
    });
}

// Main initialization function
function initGame() {
    initDOMElements();
    initThemeSystem();
    setupEventListeners();
    startNewGame();
    
    // Mobile keyboard optimization
    if ('ontouchstart' in window) {
        document.body.classList.add('touch-device');
    }
    
    console.log('Mobile number guessing game initialized');
}

// Service Worker registration (for offline functionality)
if ('serviceWorker' in navigator && window.location.protocol === 'https:') {
    window.addEventListener('load', function() {
        navigator.serviceWorker.register('./sw.js')
            .then(function(registration) {
                console.log('ServiceWorker registered:', registration.scope);
            })
            .catch(function(error) {
                console.log('ServiceWorker registration failed:', error);
            });
    });
}
