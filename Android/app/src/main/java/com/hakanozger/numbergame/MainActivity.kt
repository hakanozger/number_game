package com.hakanozger.numbergame

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputFilter
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hakanozger.numbergame.databinding.ActivityMainBinding
import com.hakanozger.numbergame.databinding.DialogRulesBinding
import com.hakanozger.numbergame.databinding.DialogThemeBinding
import com.hakanozger.numbergame.databinding.DialogLanguageBinding
import com.hakanozger.numbergame.databinding.DialogWinBinding
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
        updateHeaderTitle()
        startNewGame()
    }

    private fun setupToolbar() {
        // Custom header bar setup
        binding.btnHelp.setOnClickListener {
            showRulesDialog()
            vibrateLight()
        }
        
        binding.btnMenu.setOnClickListener {
            showMenuDialog()
            vibrateLight()
        }
    }

    // Menu handling now done in setupToolbar() method

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
        
        // Show win dialog after animation
        binding.tvStatusMessage.postDelayed({
            showWinDialog()
        }, 1500)
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
        val animator = ObjectAnimator.ofFloat(binding.etGuessInput, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun animateWin() {
        // Scale animation for the header title
        val scaleX = ObjectAnimator.ofFloat(binding.tvToolbarTitle, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.tvToolbarTitle, "scaleY", 1f, 1.1f, 1f)
        
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
        
        // Apply to header bar
        binding.headerBar.setBackgroundColor(primaryColor)
        binding.tvToolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.hacker_bg))
        binding.btnHelp.drawable?.setTint(ContextCompat.getColor(this, R.color.hacker_bg))
        binding.btnMenu.drawable?.setTint(ContextCompat.getColor(this, R.color.hacker_bg))
        
        // Apply to input field
        binding.etGuessInput.apply {
            setTextColor(textColor)
            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.input_background)
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
        
        // Apply to header bar
        binding.headerBar.setBackgroundColor(primaryColor)
        binding.tvToolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.modern_bg))
        binding.btnHelp.drawable?.setTint(ContextCompat.getColor(this, R.color.modern_bg))
        binding.btnMenu.drawable?.setTint(ContextCompat.getColor(this, R.color.modern_bg))
        
        // Apply to input field
        binding.etGuessInput.apply {
            setTextColor(textColor)
            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.input_background_modern)
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
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.hacker_bg))
            
            // Apply hacker theme to dialog window
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.dialog_background_hacker)
            )
        } else {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.modern_bg))
            
            // Apply modern theme to dialog window
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.dialog_background_modern)
            )
        }
    }
    
    // Dialog content theme application
    private fun applyThemeToDialogContent(rootView: View) {
        if (isHackerTheme) {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.hacker_bg))
            
            // Update radio button backgrounds
            rootView.findViewById<RadioButton>(R.id.radioHackerTheme)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
            }
            rootView.findViewById<RadioButton>(R.id.radioModernTheme)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
            }
            rootView.findViewById<RadioButton>(R.id.radioTurkish)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
            }
            rootView.findViewById<RadioButton>(R.id.radioEnglish)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
            }
            
            // Win dialog buttons
            rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWinClose)?.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_secondary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
                strokeColor = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_border)
            }
            rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWinNewGame)?.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_primary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_bg))
            }
        } else {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.modern_bg))
            
            // Update radio button backgrounds
            rootView.findViewById<RadioButton>(R.id.radioHackerTheme)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector_modern)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
            }
            rootView.findViewById<RadioButton>(R.id.radioModernTheme)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector_modern)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
            }
            rootView.findViewById<RadioButton>(R.id.radioTurkish)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector_modern)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
            }
            rootView.findViewById<RadioButton>(R.id.radioEnglish)?.apply {
                setBackgroundResource(R.drawable.theme_card_selector_modern)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
            }
            
            // Win dialog buttons
            rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWinClose)?.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_secondary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
                strokeColor = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_border)
            }
            rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWinNewGame)?.apply {
                backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_primary)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_bg))
            }
        }
    }
    
    // Helper methods for themed dialogs
    private fun createThemedDialog(rootView: View): AlertDialog {
        return AlertDialog.Builder(this)
            .setView(rootView)
            .setCancelable(true)
            .create()
            .apply {
                applyThemeToDialog(this, rootView)
            }
    }
    
    private fun applyThemeToDialogButtons(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            if (isHackerTheme) {
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_accent))
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_secondary))
            } else {
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_primary))
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.modern_secondary))
            }
        }
        
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            if (isHackerTheme) {
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_surface))
            } else {
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.modern_surface))
            }
        }
    }
    
    private fun createThemedDialogButton(text: String, isPrimary: Boolean, onClick: () -> Unit): com.google.android.material.button.MaterialButton {
        return com.google.android.material.button.MaterialButton(this).apply {
            this.text = text
            if (isPrimary) {
                if (isHackerTheme) {
                    backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_primary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_bg))
                } else {
                    backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_primary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_bg))
                }
            } else {
                if (isHackerTheme) {
                    backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_secondary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_text))
                } else {
                    backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_secondary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_text))
                }
            }
            setOnClickListener { onClick() }
        }
    }

    // Dialog methods
    private fun showRulesDialog() {
        val dialogBinding = DialogRulesBinding.inflate(layoutInflater)
        
        // Apply theme to dialog content
        applyThemeToDialogContent(dialogBinding.root)
        
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
        
        // Apply theme to dialog buttons after show
        applyThemeToDialogButtons(dialog)
    }
    
    private fun showThemeDialog() {
        val dialogBinding = DialogThemeBinding.inflate(layoutInflater)
        
        // Apply theme to dialog content
        applyThemeToDialogContent(dialogBinding.root)
        
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
        
        // Apply theme to dialog buttons after show
        applyThemeToDialogButtons(dialog)
    }
    
    private fun showLanguageDialog() {
        val dialogBinding = DialogLanguageBinding.inflate(layoutInflater)
        
        // Apply theme to dialog content
        applyThemeToDialogContent(dialogBinding.root)
        
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
                    // Update language without restart
                    loadLanguage()
                    updateHeaderTitle()
                    updateUI()
                    vibrateLight()
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
        
        // Apply theme to dialog buttons after show
        applyThemeToDialogButtons(dialog)
    }
    
    private fun showAboutDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage("${getString(R.string.about_description)}\n\n${getString(R.string.about_version)}\n${getString(R.string.about_developer)}")
            .setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_help)
            .setCancelable(true)
            .create()
            
        dialog.show()
        
        // Apply theme to dialog buttons after show
        applyThemeToDialogButtons(dialog)
    }
    
    private fun showWinDialog() {
        val dialogBinding = DialogWinBinding.inflate(layoutInflater)
        
        // Apply theme to dialog content
        applyThemeToDialogContent(dialogBinding.root)
        
        // Set win data
        dialogBinding.tvWinAttempts.text = attempts.toString()
        dialogBinding.tvWinSecretNumber.text = secretNumber
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false) // Prevent dismiss by tapping outside
            .create()
        
        // Setup button listeners
        dialogBinding.btnWinClose.setOnClickListener {
            dialog.dismiss()
            // Keep the game state for review
        }
        
        dialogBinding.btnWinNewGame.setOnClickListener {
            dialog.dismiss()
            startNewGame()
            vibrateLight()
        }
        
        // Apply theme to dialog
        applyThemeToDialog(dialog, dialogBinding.root)
        dialog.show()
    }
    
    private fun showMenuDialog() {
        val menuItems = arrayOf(
            getString(R.string.menu_theme),
            getString(R.string.menu_language),
            getString(R.string.menu_about)
        )
        
        AlertDialog.Builder(this)
            .setTitle("Menü")
            .setItems(menuItems) { dialog, which ->
                when (which) {
                    0 -> showThemeDialog()
                    1 -> showLanguageDialog()
                    2 -> showAboutDialog()
                }
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .apply {
                show()
                applyThemeToDialogButtons(this)
            }
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
        
        // Update app title in status bar
        title = getString(R.string.app_name)
    }
    
    private fun updateHeaderTitle() {
        // Update header title with current language
        binding.tvToolbarTitle.text = getString(R.string.game_title)
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
