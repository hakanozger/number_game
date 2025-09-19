package com.hakanozger.numbergame

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputFilter
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.TextView
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
    private var historyDialog: AlertDialog? = null
    
    // Game variables
    private var secretNumber = ""
    private var attempts = 0
    private var gameHistory = mutableListOf<GuessResult>()
    private var isHackerTheme = true
    private val maxHistorySize = 10 // Maksimum gösterilecek tahmin sayısı

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable full screen mode
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide action bar if exists
        supportActionBar?.hide()
        
        // Modern immersive mode (Android 11+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Legacy full screen mode
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
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
        
        // Setup History Adapter (will be used in popup)
        historyAdapter = HistoryAdapter(gameHistory)
        
        // Setup last guess display
        updateLastGuessDisplay()
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
        
        // Clear button removed - functionality integrated into backspace
        
        // Show history button
        binding.btnShowHistory.setOnClickListener {
            showHistoryDialog()
            vibrateLight()
        }
        
        // Setup custom numpad
        setupNumpad()

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
        updateLastGuessDisplay()
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
        updateLastGuessDisplay()
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
        updateLastGuessDisplay()
    }
    
    private fun updateHistoryLimitMessage() {
        // History limit message is now only shown in the popup dialog
        // This method is kept for compatibility but does nothing on main screen
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
        binding.tvStatusMessage.setTextColor(textColor)
        binding.tvAttempts.setTextColor(textColor)
        
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
        binding.btnGuess.apply {
            backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.success_green)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }
        
        // Apply to cards
        binding.gameControlsCard.setCardBackgroundColor(surfaceColor)
        binding.gameStatusCard.setCardBackgroundColor(secondaryColor)
        binding.lastGuessCard.setCardBackgroundColor(surfaceColor)
        binding.numpadCard.setCardBackgroundColor(surfaceColor)
        
        // Apply to last guess container and header
        binding.lastGuessHeader.setBackgroundColor(secondaryColor)
        binding.lastGuessContainer.setBackgroundColor(secondaryColor)
        
        historyAdapter.updateTheme(true)
        
        // Apply theme to numpad buttons
        applyNumpadTheme(true)
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
        binding.tvStatusMessage.setTextColor(textColor)
        binding.tvAttempts.setTextColor(textColor)
        
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
        binding.btnGuess.apply {
            backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.success_green)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }
        
        // Apply to cards
        binding.gameControlsCard.setCardBackgroundColor(surfaceColor)
        binding.gameStatusCard.setCardBackgroundColor(secondaryColor)
        binding.lastGuessCard.setCardBackgroundColor(surfaceColor)
        binding.numpadCard.setCardBackgroundColor(surfaceColor)
        
        // Apply to last guess container and header
        binding.lastGuessHeader.setBackgroundColor(secondaryColor)
        binding.lastGuessContainer.setBackgroundColor(secondaryColor)
        
        historyAdapter.updateTheme(false)
        
        // Apply theme to numpad buttons
        applyNumpadTheme(false)
    }
    
    private fun applyNumpadTheme(isHacker: Boolean) {
        val numpadButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        
        if (isHacker) {
            val buttonColor = ContextCompat.getColor(this, R.color.hacker_primary)
            val textColor = ContextCompat.getColor(this, R.color.hacker_bg)
            
            numpadButtons.forEach { button ->
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(buttonColor)
                button.setTextColor(textColor)
            }
        } else {
            val buttonColor = ContextCompat.getColor(this, R.color.modern_primary)
            val textColor = ContextCompat.getColor(this, R.color.white)
            
            numpadButtons.forEach { button ->
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(buttonColor)
                button.setTextColor(textColor)
            }
        }
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
                    updateUITexts()
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
            getString(R.string.menu_new_game),
            getString(R.string.menu_theme),
            getString(R.string.menu_language),
            getString(R.string.menu_about)
        )
        
        AlertDialog.Builder(this)
            .setTitle("Menü")
            .setItems(menuItems) { dialog, which ->
                when (which) {
                    0 -> {
                        startNewGame()
                        vibrateLight()
                    }
                    1 -> showThemeDialog()
                    2 -> showLanguageDialog()
                    3 -> showAboutDialog()
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
        
        // Status bar removed - no title needed
    }
    
    private fun updateHeaderTitle() {
        // Update header title with current language
        binding.tvToolbarTitle.text = getString(R.string.game_title)
    }
    
    private fun updateUITexts() {
        // Update all UI texts with current language
        updateHeaderTitle()
        
        // Update button texts
        binding.btnGuess.text = "✓"
        binding.btnShowHistory.text = getString(R.string.btn_show_history)
        
        // Update status message
        updateUI()
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
    
    private fun updateLastGuessDisplay() {
        if (gameHistory.isEmpty()) {
            binding.lastGuessHeader.visibility = View.GONE
            binding.lastGuessContainer.visibility = View.GONE
        } else {
            val lastGuess = gameHistory[0]
            binding.lastGuessHeader.visibility = View.VISIBLE
            binding.lastGuessContainer.visibility = View.VISIBLE
            binding.tvLastGuess.text = lastGuess.guess
            binding.tvLastCorrectDigits.text = lastGuess.correctDigits.toString()
            binding.tvLastCorrectPositions.text = lastGuess.correctPositions.toString()
        }
    }
    
    private fun setupNumpad() {
        val numpadButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        
        // Number buttons (0-9)
        numpadButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                onNumpadClick(index.toString())
                vibrateLight()
            }
        }
        
        // Clear button now in numpad bottom row
        
        // Backspace button (single tap: backspace, long press: clear all)
        binding.btnBackspace.setOnClickListener {
            onNumpadBackspace()
            vibrateLight()
        }
        
        binding.btnBackspace.setOnLongClickListener {
            onNumpadClear()
            vibrateLight()
            true
        }
    }
    
    private fun onNumpadClick(digit: String) {
        val currentText = binding.etGuessInput.text.toString()
        
        // Check if we already have 4 digits
        if (currentText.length >= 4) {
            return
        }
        
        // Check if digit already exists (no duplicates allowed)
        if (currentText.contains(digit)) {
            // Show brief error feedback
            binding.etGuessInput.error = getString(R.string.invalid_input_duplicate)
            binding.etGuessInput.postDelayed({ binding.etGuessInput.error = null }, 1500)
            return
        }
        
        // Add digit to input
        binding.etGuessInput.setText(currentText + digit)
    }
    
    private fun onNumpadBackspace() {
        val currentText = binding.etGuessInput.text.toString()
        if (currentText.isNotEmpty()) {
            binding.etGuessInput.setText(currentText.dropLast(1))
        }
    }
    
    private fun onNumpadClear() {
        binding.etGuessInput.setText("")
        binding.etGuessInput.error = null
    }
    
    private fun showHistoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_history, null)
        
        // Setup RecyclerView in dialog
        val rvHistoryPopup = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHistoryPopup)
        val tvEmptyHistory = dialogView.findViewById<TextView>(R.id.tvEmptyHistory)
        val tvHistoryLimitPopup = dialogView.findViewById<TextView>(R.id.tvHistoryLimitPopup)
        // Clear history button removed from popup for better UX
        val btnCloseHistory = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseHistory)
        
        rvHistoryPopup.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
        
        // Show/hide empty state
        if (gameHistory.isEmpty()) {
            rvHistoryPopup.visibility = View.GONE
            tvEmptyHistory.visibility = View.VISIBLE
            // Clear history button removed
        } else {
            rvHistoryPopup.visibility = View.VISIBLE
            tvEmptyHistory.visibility = View.GONE
            
            // Show limit message if needed
            if (gameHistory.size >= maxHistorySize) {
                tvHistoryLimitPopup.text = getString(R.string.history_limit_message, maxHistorySize)
                tvHistoryLimitPopup.visibility = View.VISIBLE
            } else {
                tvHistoryLimitPopup.visibility = View.GONE
            }
        }
        
        // Clear history button removed for better UX
        
        // Close button
        btnCloseHistory.setOnClickListener {
            historyDialog?.dismiss()
        }
        
        historyDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        applyThemeToDialog(historyDialog!!, dialogView)
        historyDialog?.show()
    }
}

// Data class for guess results
data class GuessResult(
    val guess: String,
    val correctDigits: Int,
    val correctPositions: Int
)
