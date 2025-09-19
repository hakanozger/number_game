package com.hakanozger.numbergame

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputFilter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hakanozger.numbergame.databinding.ActivityMainBinding
import com.hakanozger.numbergame.databinding.DialogRulesBinding
import com.hakanozger.numbergame.databinding.DialogThemeBinding
import com.hakanozger.numbergame.databinding.DialogLanguageBinding
import java.util.Locale
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
    private val maxHistorySize = 10 // Maksimum gösterilecek tahmin sayısı

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load saved language
        loadLanguage()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeComponents()
        setupUI()
        setupEventListeners()
        loadTheme()
        startNewGame()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            showRulesDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_theme -> {
                showThemeDialog()
                true
            }
            R.id.menu_language -> {
                showLanguageDialog()
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        // Game buttons
        binding.btnGuess.setOnClickListener {
            submitGuess()
        }
        
        binding.btnNewGame.setOnClickListener {
            startNewGame()
            vibrateLight()
        }
        
        // Clear history button
        binding.btnClearHistory.setOnClickListener {
            clearHistory()
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
            vibrateError()
            shakeInput()
            return
        }
        
        attempts++
        val result = evaluateGuess(guess)
        
        // Add to beginning and limit size
        gameHistory.add(0, result)
        if (gameHistory.size > maxHistorySize) {
            gameHistory.removeAt(gameHistory.size - 1)
            historyAdapter.notifyItemRemoved(gameHistory.size)
        }
        
        vibrateLight()
        updateUI()
        updateHistoryLimitMessage()
        historyAdapter.notifyItemInserted(0)
        
        // Scroll to top to show new item
        binding.rvHistory.scrollToPosition(0)
        
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
        if (guess.length != 4) {
            showError(getString(R.string.invalid_input))
            return false
        }
        if (!guess.all { it.isDigit() }) {
            showError(getString(R.string.invalid_input))
            return false
        }
        
        // Check for unique digits
        if (guess.toSet().size != 4) {
            showError(getString(R.string.invalid_input_duplicate))
            return false
        }
        
        return true
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
        // Scale animation for the toolbar title
        val scaleX = ObjectAnimator.ofFloat(binding.toolbar, "scaleX", 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.toolbar, "scaleY", 1f, 1.05f, 1f)
        
        scaleX.duration = 1000
        scaleY.duration = 1000
        scaleX.repeatCount = 2
        scaleY.repeatCount = 2
        
        scaleX.start()
        scaleY.start()
    }

    // New methods for mobile UI
    private fun clearHistory() {
        gameHistory.clear()
        historyAdapter.notifyDataSetChanged()
        updateHistoryLimitMessage()
    }
    
    private fun updateHistoryLimitMessage() {
        if (gameHistory.size >= maxHistorySize) {
            binding.tvHistoryLimit.text = getString(R.string.history_limit_message, maxHistorySize)
            binding.tvHistoryLimit.visibility = View.VISIBLE
        } else {
            binding.tvHistoryLimit.visibility = View.GONE
        }
    }

    // Theme switching methods
    private fun switchToHackerTheme() {
        isHackerTheme = true
        saveTheme(true)
        applyHackerTheme()
    }

    private fun switchToModernTheme() {
        isHackerTheme = false
        saveTheme(false)
        applyModernTheme()
    }

    private fun applyHackerTheme() {
        val bgColor = ContextCompat.getColor(this, R.color.hacker_bg)
        val textColor = ContextCompat.getColor(this, R.color.hacker_text)
        val primaryColor = ContextCompat.getColor(this, R.color.hacker_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.hacker_secondary)
        val surfaceColor = ContextCompat.getColor(this, R.color.hacker_surface)
        val inputBgColor = ContextCompat.getColor(this, R.color.hacker_input_bg)
        val borderColor = ContextCompat.getColor(this, R.color.hacker_border)
        
        // Apply colors to main elements
        binding.main.setBackgroundColor(bgColor)
        binding.tvGameDescription.setTextColor(textColor)
        binding.tvStatusMessage.setTextColor(textColor)
        binding.tvAttempts.setTextColor(textColor)
        binding.tvHistoryTitle.setTextColor(textColor)
        binding.tvHistoryLimit.setTextColor(textColor)
        
        // Apply to toolbar and app bar
        val appBarLayout = binding.toolbar.parent as com.google.android.material.appbar.AppBarLayout
        appBarLayout.setBackgroundColor(primaryColor)
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.hacker_bg))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.hacker_bg))
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.hacker_bg))
        
        // Apply to input field
        binding.etGuessInput.setTextColor(textColor)
        binding.etGuessInput.setHintTextColor(ContextCompat.getColor(this, R.color.hacker_text))
        binding.inputLayout.apply {
            setBoxBackgroundColor(inputBgColor)
            setBoxStrokeColor(borderColor)
            setHintTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_text))
        }
        
        // Apply to buttons
        binding.btnGuess.apply {
            backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_primary)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_bg))
        }
        binding.btnNewGame.apply {
            backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_secondary)
            setTextColor(textColor)
            strokeColor = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_border)
        }
        
        // Apply to cards
        binding.gameControlsCard.setCardBackgroundColor(surfaceColor)
        binding.gameStatusCard.setCardBackgroundColor(secondaryColor)
        binding.gameHistoryCard.setCardBackgroundColor(surfaceColor)
        
        // Apply to history header
        binding.historyHeader.setBackgroundColor(secondaryColor)
        
        historyAdapter.updateTheme(true)
    }

    private fun applyModernTheme() {
        val bgColor = ContextCompat.getColor(this, R.color.modern_bg)
        val textColor = ContextCompat.getColor(this, R.color.modern_text)
        val primaryColor = ContextCompat.getColor(this, R.color.modern_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.modern_secondary)
        val surfaceColor = ContextCompat.getColor(this, R.color.modern_surface)
        val inputBgColor = ContextCompat.getColor(this, R.color.modern_input_bg)
        val borderColor = ContextCompat.getColor(this, R.color.modern_border)
        
        // Apply colors to main elements
        binding.main.setBackgroundColor(bgColor)
        binding.tvGameDescription.setTextColor(textColor)
        binding.tvStatusMessage.setTextColor(textColor)
        binding.tvAttempts.setTextColor(textColor)
        binding.tvHistoryTitle.setTextColor(textColor)
        binding.tvHistoryLimit.setTextColor(textColor)
        
        // Apply to toolbar and app bar
        val appBarLayout = binding.toolbar.parent as com.google.android.material.appbar.AppBarLayout
        appBarLayout.setBackgroundColor(primaryColor)
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.modern_bg))
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.modern_bg))
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.modern_bg))
        
        // Apply to input field
        binding.etGuessInput.setTextColor(textColor)
        binding.etGuessInput.setHintTextColor(ContextCompat.getColor(this, R.color.modern_text))
        binding.inputLayout.apply {
            setBoxBackgroundColor(inputBgColor)
            setBoxStrokeColor(borderColor)
            setHintTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.modern_text))
        }
        
        // Apply to buttons
        binding.btnGuess.apply {
            backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_primary)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_bg))
        }
        binding.btnNewGame.apply {
            backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_secondary)
            setTextColor(textColor)
            strokeColor = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_border)
        }
        
        // Apply to cards
        binding.gameControlsCard.setCardBackgroundColor(surfaceColor)
        binding.gameStatusCard.setCardBackgroundColor(secondaryColor)
        binding.gameHistoryCard.setCardBackgroundColor(surfaceColor)
        
        // Apply to history header
        binding.historyHeader.setBackgroundColor(secondaryColor)
        
        historyAdapter.updateTheme(false)
    }

    // Dialog theme application
    private fun applyThemeToDialog(dialog: AlertDialog, rootView: View) {
        if (isHackerTheme) {
            dialog.window?.setBackgroundDrawableResource(R.color.hacker_surface)
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.hacker_surface))
        } else {
            dialog.window?.setBackgroundDrawableResource(R.color.modern_surface)
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.modern_surface))
        }
        
        // Apply corner radius
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.dialog_background)
        )
    }

    // Dialog methods
    private fun showRulesDialog() {
        val dialogBinding = DialogRulesBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
        
        // Apply theme to dialog
        applyThemeToDialog(dialog, dialogBinding.root)
        dialog.show()
    }
    
    private fun showThemeDialog() {
        val dialogBinding = DialogThemeBinding.inflate(layoutInflater)
        
        // Set current theme
        if (isHackerTheme) {
            dialogBinding.radioHackerTheme.isChecked = true
        } else {
            dialogBinding.radioModernTheme.isChecked = true
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.dialog_apply)) { dialog, _ ->
                // Apply selected theme
                when (dialogBinding.themeRadioGroup.checkedRadioButtonId) {
                    R.id.radioHackerTheme -> {
                        if (!isHackerTheme) {
                            switchToHackerTheme()
                            vibrateLight()
                        }
                    }
                    R.id.radioModernTheme -> {
                        if (isHackerTheme) {
                            switchToModernTheme()
                            vibrateLight()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            
        // Apply theme to dialog
        applyThemeToDialog(dialog, dialogBinding.root)
        dialog.show()
    }
    
    private fun showLanguageDialog() {
        val dialogBinding = DialogLanguageBinding.inflate(layoutInflater)
        
        // Set current language
        val currentLanguage = sharedPreferences.getString("app_language", "tr") ?: "tr"
        if (currentLanguage == "tr") {
            dialogBinding.radioTurkish.isChecked = true
        } else {
            dialogBinding.radioEnglish.isChecked = true
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.dialog_apply)) { dialog, _ ->
                val selectedLanguage = when (dialogBinding.languageRadioGroup.checkedRadioButtonId) {
                    R.id.radioTurkish -> "tr"
                    R.id.radioEnglish -> "en"
                    else -> "tr"
                }
                
                if (selectedLanguage != currentLanguage) {
                    saveLanguage(selectedLanguage)
                    restartApp()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            
        // Apply theme to dialog
        applyThemeToDialog(dialog, dialogBinding.root)
        dialog.show()
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage("${getString(R.string.about_description)}\n\n${getString(R.string.about_version)}\n${getString(R.string.about_developer)}")
            .setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_help)
            .setCancelable(true)
            .show()
    }

    private fun loadTheme() {
        isHackerTheme = sharedPreferences.getBoolean("is_hacker_theme", true)
        if (isHackerTheme) {
            applyHackerTheme()
        } else {
            applyModernTheme()
        }
    }

    private fun saveTheme(isHacker: Boolean) {
        sharedPreferences.edit()
            .putBoolean("is_hacker_theme", isHacker)
            .apply()
    }
    
    // Language management methods
    private fun saveLanguage(language: String) {
        sharedPreferences.edit()
            .putString("app_language", language)
            .apply()
    }
    
    private fun loadLanguage() {
        val savedLanguage = getSharedPreferences("NumberGamePrefs", Context.MODE_PRIVATE)
            .getString("app_language", "tr") ?: "tr"
        
        val locale = Locale(savedLanguage)
        Locale.setDefault(locale)
        
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
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
