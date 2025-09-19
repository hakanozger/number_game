package com.hakanozger.numbergame

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputFilter
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hakanozger.numbergame.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator
    
    // Game variables
    private var secretNumber = ""
    private var attempts = 0
    private var gameHistory = mutableListOf<GuessResult>()
    private var isHackerTheme = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupUI()
        setupEventListeners()
        loadTheme()
        startNewGame()
    }

    private fun initializeComponents() {
        sharedPreferences = getSharedPreferences("NumberGamePrefs", Context.MODE_PRIVATE)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Setup RecyclerView
        historyAdapter = HistoryAdapter(gameHistory)
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun setupUI() {
        // Input restrictions
        binding.etGuessInput.filters = arrayOf(InputFilter.LengthFilter(4))
        
        // Initially focus on input
        binding.etGuessInput.requestFocus()
    }

    private fun setupEventListeners() {
        // Theme buttons
        binding.btnHackerTheme.setOnClickListener {
            if (!isHackerTheme) {
                switchToHackerTheme()
                vibrateLight()
            }
        }
        
        binding.btnModernTheme.setOnClickListener {
            if (isHackerTheme) {
                switchToModernTheme()
                vibrateLight()
            }
        }

        // Game buttons
        binding.btnGuess.setOnClickListener {
            submitGuess()
        }
        
        binding.btnNewGame.setOnClickListener {
            startNewGame()
            vibrateLight()
        }

        // Input field - submit on Enter
        binding.etGuessInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitGuess()
                true
            } else {
                false
            }
        }
    }

    private fun generateSecretNumber(): String {
        val digits = mutableListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val result = StringBuilder()
        
        // First digit should not be 0
        val firstDigit = Random.nextInt(1, 10)
        result.append(firstDigit)
        digits.remove(firstDigit)
        
        // Add remaining 3 digits
        repeat(3) {
            val randomIndex = Random.nextInt(digits.size)
            result.append(digits[randomIndex])
            digits.removeAt(randomIndex)
        }
        
        return result.toString()
    }

    private fun startNewGame() {
        secretNumber = generateSecretNumber()
        attempts = 0
        gameHistory.clear()
        
        updateUI()
        historyAdapter.notifyDataSetChanged()
        
        binding.etGuessInput.apply {
            text?.clear()
            isEnabled = true
            requestFocus()
        }
        
        binding.btnGuess.isEnabled = true
        
        // Debug log (remove in production)
        println("Secret number: $secretNumber")
    }

    private fun submitGuess() {
        val guess = binding.etGuessInput.text.toString().trim()
        
        // Validate input
        if (!isValidGuess(guess)) {
            showError(getString(R.string.invalid_input))
            vibrateError()
            shakeInput()
            return
        }
        
        attempts++
        val result = evaluateGuess(guess)
        gameHistory.add(0, result) // Add to beginning for reverse chronological order
        
        vibrateLight()
        updateUI()
        historyAdapter.notifyItemInserted(0)
        
        // Check win condition
        if (result.correctPositions == 4) {
            handleWin()
        } else {
            binding.etGuessInput.apply {
                text?.clear()
                requestFocus()
            }
        }
    }

    private fun isValidGuess(guess: String): Boolean {
        if (guess.length != 4) return false
        if (!guess.all { it.isDigit() }) return false
        
        // Check for unique digits
        return guess.toSet().size == 4
    }

    private fun evaluateGuess(guess: String): GuessResult {
        var correctDigits = 0
        var correctPositions = 0
        
        // Calculate correct positions
        for (i in 0..3) {
            if (guess[i] == secretNumber[i]) {
                correctPositions++
            }
        }
        
        // Calculate correct digits
        val secretDigits = secretNumber.toMutableList()
        val guessDigits = guess.toMutableList()
        
        for (digit in guessDigits) {
            if (secretDigits.contains(digit)) {
                correctDigits++
                secretDigits.remove(digit)
            }
        }
        
        return GuessResult(guess, correctDigits, correctPositions)
    }

    private fun handleWin() {
        binding.etGuessInput.isEnabled = false
        binding.btnGuess.isEnabled = false
        
        val winMessage = getString(R.string.win_message, attempts, secretNumber)
        binding.tvStatusMessage.text = winMessage
        
        // Win animation
        animateWin()
        vibrateWin()
        
        // Show win toast
        Toast.makeText(this, getString(R.string.congratulations), Toast.LENGTH_LONG).show()
    }

    private fun updateUI() {
        binding.tvAttempts.text = getString(R.string.attempts_count, attempts)
        
        val statusMessage = if (attempts == 0) {
            getString(R.string.game_started)
        } else {
            val lastGuess = gameHistory.firstOrNull()
            if (lastGuess != null && lastGuess.correctPositions != 4) {
                getString(R.string.guess_result, lastGuess.guess)
            } else {
                binding.tvStatusMessage.text
            }
        }
        
        binding.tvStatusMessage.text = statusMessage
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun shakeInput() {
        val animator = ObjectAnimator.ofFloat(binding.inputLayout, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun animateWin() {
        // Scale animation for the title
        val scaleX = ObjectAnimator.ofFloat(binding.tvGameTitle, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.tvGameTitle, "scaleY", 1f, 1.1f, 1f)
        
        scaleX.duration = 1000
        scaleY.duration = 1000
        scaleX.repeatCount = 2
        scaleY.repeatCount = 2
        
        scaleX.start()
        scaleY.start()
    }

    // Theme switching methods
    private fun switchToHackerTheme() {
        isHackerTheme = true
        saveTheme(true)
        applyHackerTheme()
        updateThemeButtons()
    }

    private fun switchToModernTheme() {
        isHackerTheme = false
        saveTheme(false)
        applyModernTheme()
        updateThemeButtons()
    }

    private fun applyHackerTheme() {
        val bgColor = ContextCompat.getColor(this, R.color.hacker_bg)
        val textColor = ContextCompat.getColor(this, R.color.hacker_text)
        val primaryColor = ContextCompat.getColor(this, R.color.hacker_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.hacker_secondary)
        val surfaceColor = ContextCompat.getColor(this, R.color.hacker_surface)
        
        // Apply colors to main elements
        binding.main.setBackgroundColor(bgColor)
        binding.tvGameTitle.setTextColor(textColor)
        binding.tvGameDescription.setTextColor(textColor)
        binding.tvStatusMessage.setTextColor(textColor)
        binding.tvAttempts.setTextColor(textColor)
        binding.tvHistoryTitle.setTextColor(textColor)
        binding.tvRulesTitle.setTextColor(textColor)
        
        // Apply to cards
        binding.gameControlsCard.setCardBackgroundColor(surfaceColor)
        binding.gameStatusCard.setCardBackgroundColor(secondaryColor)
        binding.gameHistoryCard.setCardBackgroundColor(surfaceColor)
        binding.gameRulesCard.setCardBackgroundColor(secondaryColor)
        
        historyAdapter.updateTheme(true)
    }

    private fun applyModernTheme() {
        val bgColor = ContextCompat.getColor(this, R.color.modern_bg)
        val textColor = ContextCompat.getColor(this, R.color.modern_text)
        val primaryColor = ContextCompat.getColor(this, R.color.modern_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.modern_secondary)
        val surfaceColor = ContextCompat.getColor(this, R.color.modern_surface)
        
        // Apply colors to main elements
        binding.main.setBackgroundColor(bgColor)
        binding.tvGameTitle.setTextColor(textColor)
        binding.tvGameDescription.setTextColor(textColor)
        binding.tvStatusMessage.setTextColor(textColor)
        binding.tvAttempts.setTextColor(textColor)
        binding.tvHistoryTitle.setTextColor(textColor)
        binding.tvRulesTitle.setTextColor(textColor)
        
        // Apply to cards
        binding.gameControlsCard.setCardBackgroundColor(surfaceColor)
        binding.gameStatusCard.setCardBackgroundColor(secondaryColor)
        binding.gameHistoryCard.setCardBackgroundColor(surfaceColor)
        binding.gameRulesCard.setCardBackgroundColor(secondaryColor)
        
        historyAdapter.updateTheme(false)
    }

    private fun updateThemeButtons() {
        if (isHackerTheme) {
            // Hacker theme active
            binding.btnHackerTheme.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_primary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_bg))
            }
            binding.btnModernTheme.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_secondary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
            }
        } else {
            // Modern theme active
            binding.btnModernTheme.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_primary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_bg))
            }
            binding.btnHackerTheme.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_secondary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
            }
        }
    }

    private fun loadTheme() {
        isHackerTheme = sharedPreferences.getBoolean("is_hacker_theme", true)
        if (isHackerTheme) {
            applyHackerTheme()
        } else {
            applyModernTheme()
        }
        updateThemeButtons()
    }

    private fun saveTheme(isHacker: Boolean) {
        sharedPreferences.edit()
            .putBoolean("is_hacker_theme", isHacker)
            .apply()
    }

    // Vibration methods
    private fun vibrateLight() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun vibrateError() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    private fun vibrateWin() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            vibrator.vibrate(pattern, -1)
        }
    }
}

// Data class for guess results
data class GuessResult(
    val guess: String,
    val correctDigits: Int,
    val correctPositions: Int
)
