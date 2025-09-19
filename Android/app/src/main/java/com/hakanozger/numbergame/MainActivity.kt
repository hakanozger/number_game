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
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hakanozger.numbergame.databinding.ActivityMainBinding
import com.hakanozger.numbergame.databinding.DialogRulesBinding
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
    private var currentInput = mutableListOf<String>()
    private val digitViews by lazy { listOf(binding.digit1, binding.digit2, binding.digit3, binding.digit4) }
    
    // Game variables
    private var secretNumber = ""
    private var attempts = 0
    private var gameHistory = mutableListOf<GuessResult>()
    // App uses only Hacker theme (simplified)
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
        applyHackerTheme() // Only Hacker theme
        updateHeaderTitle()
        startNewGame()
    }

    private fun setupToolbar() {
        // Custom header bar setup
        binding.btnHelp.setOnClickListener {
            showMenuDialog()
            vibrateIfEnabled()
        }
        
        binding.btnMenu.setOnClickListener {
            showMenuDialog()
            vibrateIfEnabled()
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
        // Input now handled by 4-box system
        // No need for EditText restrictions or focus
        updateDigitBoxes()
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
            vibrateIfEnabled()
        }
        
        // Setup custom numpad
        setupNumpad()

        // Input now handled by 4-box system and numpad
        // No EditText editor actions needed
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
        
        currentInput.clear()
        updateDigitBoxes()
        
        binding.btnGuess.isEnabled = true
        
        // Debug log (remove in production)
        println("Secret number: $secretNumber")
    }

    private fun submitGuess() {
        val guess = getCurrentGuess()
        
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
        
        vibrateIfEnabled()
        updateUI()
        updateHistoryLimitMessage()
        updateLastGuessDisplay()
        historyAdapter.notifyItemInserted(0)
        
        // Check win condition
        if (result.correctPositions == 4) {
            handleWin()
        } else {
            // Clear input using new 4-box system
            onNumpadClear()
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

    // shakeInput method moved to new location with 4-box implementation

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

    // Theme methods removed - app uses only Hacker theme

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
        
        // Apply to digit input boxes
        digitViews.forEach { digitView ->
            digitView.setTextColor(textColor)
        }
        
        // Apply to header bar - use XML drawable for glass effect
        // binding.headerBar.setBackgroundColor(primaryColor) // Removed to use XML drawable
        binding.tvToolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.white))
        // PNG logo - no tinting needed (has own colors)
        binding.btnMenu.drawable?.setTint(ContextCompat.getColor(this, R.color.white))
        
        // Apply to digit input boxes (already handled in digitViews.forEach above)
        
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
        
        // Apply glass theme to History button
        binding.btnShowHistory.apply {
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
            backgroundTintList = null // Remove tint to show glass effect
            iconTint = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_primary)
        }
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
        
        // Apply to digit input boxes
        digitViews.forEach { digitView ->
            digitView.setTextColor(textColor)
        }
        
        // Apply to header bar - use XML drawable for glass effect
        // binding.headerBar.setBackgroundColor(primaryColor) // Removed to use XML drawable
        binding.tvToolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.modern_bg))
        binding.btnHelp.drawable?.setTint(ContextCompat.getColor(this, R.color.modern_primary))
        binding.btnMenu.drawable?.setTint(ContextCompat.getColor(this, R.color.modern_bg))
        
        // Apply to digit input boxes (already handled in digitViews.forEach above)
        
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
        // Glass theme is now applied directly in layout XML
        // Just ensure text colors are correct and remove any background tints
        val numpadButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        
        // Ensure numpad buttons have green text for visibility on glass
        numpadButtons.forEach { button ->
            button.setTextColor(ContextCompat.getColor(this, R.color.hacker_primary))
            button.backgroundTintList = null // Remove any tint to show glass effect
        }
        
        // Ensure checkmark button has white text on green glass
        binding.btnGuess.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.btnGuess.backgroundTintList = null
        
        // Backspace button now uses glass effect (red glass) with green text
        binding.btnBackspace.setTextColor(ContextCompat.getColor(this, R.color.hacker_primary))
        binding.btnBackspace.backgroundTintList = null // Remove tint to show red glass effect
    }

    // Dialog theme application
    private fun applyThemeToDialog(dialog: AlertDialog, rootView: View) {
        // Always use Hacker theme
        if (true) {
            // Use XML layout background (dialog_glass_background) - don't override
            // rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.hacker_bg))
            
            // Apply glass theme to dialog window
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.dialog_glass_background)
            )
        } else {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.modern_bg))
            
            // Apply modern theme to dialog window
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.dialog_background_modern)
            )
        }
    }
    
    // Dialog content theme application (Hacker theme only)
    private fun applyThemeToDialogContent(rootView: View) {
        // Use XML layout background (dialog_glass_background) - don't override
        // rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.hacker_bg))
        
        // Apply Glass theme to language radio buttons
        rootView.findViewById<RadioButton>(R.id.radioTurkish)?.apply {
            setBackgroundResource(R.drawable.language_button_selector)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
        }
        rootView.findViewById<RadioButton>(R.id.radioEnglish)?.apply {
            setBackgroundResource(R.drawable.language_button_selector)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
        }
        
        // Win dialog buttons (Glass theme)
        rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWinClose)?.apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            backgroundTintList = null
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
        }
        rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWinNewGame)?.apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            backgroundTintList = null
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
        }
        
        // History dialog button (Glass theme)
        rootView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseHistory)?.apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            backgroundTintList = null
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
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
            // Apply glass theme to dialog buttons
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
        }
        
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
        }
        
    }
    
    private fun createThemedDialogButton(text: String, isPrimary: Boolean, onClick: () -> Unit): com.google.android.material.button.MaterialButton {
        return com.google.android.material.button.MaterialButton(this).apply {
            this.text = text
            if (isPrimary) {
                // Always use Hacker theme
        if (true) {
                    backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.hacker_primary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_bg))
                } else {
                    backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.modern_primary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.modern_bg))
                }
            } else {
                // Always use Hacker theme
        if (true) {
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

    // Dialog methods moved to bottom of class
    
    // Theme dialog removed - app uses only Hacker theme
    
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
                    vibrateIfEnabled()
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
    
    private fun showVibrationDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        val title = TextView(this).apply {
            text = getString(R.string.dialog_vibration_title)
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)
        
        // Current setting
        val currentSetting = isVibrationEnabled()
        
        // Create buttons first, then set click listeners with proper references
        val enableButton = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.vibration_enabled)
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            backgroundTintList = null
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
            alpha = if (currentSetting) 1.0f else 0.5f
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            layoutParams = params
        }
        
        val disableButton = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.vibration_disabled)
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.dialog_button_selector)
            backgroundTintList = null
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
            alpha = if (!currentSetting) 1.0f else 0.5f
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            layoutParams = params
        }
        
        // Now set click listeners with proper button references
        enableButton.setOnClickListener { 
            saveVibrationSetting(true)
            // Update visual feedback
            enableButton.alpha = 1.0f
            disableButton.alpha = 0.5f
            vibrateIfEnabled()
        }
        
        disableButton.setOnClickListener { 
            saveVibrationSetting(false)
            // Update visual feedback
            disableButton.alpha = 1.0f
            enableButton.alpha = 0.5f
        }
        
        layout.addView(enableButton)
        layout.addView(disableButton)
        
        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()
            
        applyThemeToDialog(dialog, layout)
        dialog.show()
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
            vibrateIfEnabled()
        }
        
        // Apply theme to dialog
        applyThemeToDialog(dialog, dialogBinding.root)
        dialog.show()
    }
    
    private fun showMenuDialog() {
        // Create custom layout with glass theme buttons
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Menu title
        val title = TextView(this).apply {
            text = "Menü"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)
        
        // Create glass theme buttons
        val menuItems = arrayOf(
            getString(R.string.menu_new_game) to { startNewGame(); vibrateIfEnabled() },
            getString(R.string.menu_language) to { showLanguageDialog() },
            getString(R.string.menu_vibration) to { showVibrationDialog() },
            getString(R.string.menu_rules) to { showRulesDialog() },
            getString(R.string.menu_about) to { showAboutDialog() }
        )
        
        menuItems.forEach { (text, action) ->
            val button = com.google.android.material.button.MaterialButton(this).apply {
                this.text = text
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.hacker_primary))
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.menu_button_selector)
                backgroundTintList = null
                typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                layoutParams = params
                
                setOnClickListener { 
                    action()
                    vibrateIfEnabled()
                }
            }
            layout.addView(button)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()
            
        applyThemeToDialog(dialog, layout)
        dialog.show()
    }

    private fun loadTheme() {
        // Always use Hacker theme (simplified)
        applyHackerTheme()
    }

    // Theme saving removed - app uses only Hacker theme
    
    // Vibration management methods
    private fun isVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean("vibration_enabled", true)
    }
    
    private fun saveVibrationSetting(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("vibration_enabled", enabled)
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
    private fun vibrateIfEnabled() {
        if (isVibrationEnabled()) {
            vibrateLight()
        }
    }
    
    private fun vibrateLight() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun vibrateError() {
        if (isVibrationEnabled()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        }
    }

    private fun vibrateWin() {
        if (isVibrationEnabled()) {
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
                vibrateIfEnabled()
            }
        }
        
        // Clear button now in numpad bottom row
        
        // Backspace button (single tap: backspace, long press: clear all)
        binding.btnBackspace.setOnClickListener {
            onNumpadBackspace()
            vibrateIfEnabled()
        }
        
        binding.btnBackspace.setOnLongClickListener {
            onNumpadClear()
            vibrateIfEnabled()
            true
        }
    }
    
    private fun onNumpadClick(digit: String) {
        // Check if we already have 4 digits
        if (currentInput.size >= 4) {
            return
        }
        
        // Check if digit already exists (no duplicates allowed)
        if (currentInput.contains(digit)) {
            // Show brief error feedback with toast
            Toast.makeText(this, getString(R.string.invalid_input_duplicate), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Add digit to input
        currentInput.add(digit)
        updateDigitBoxes()
    }
    
    private fun onNumpadBackspace() {
        if (currentInput.isNotEmpty()) {
            currentInput.removeAt(currentInput.size - 1)
            updateDigitBoxes()
        }
    }
    
    private fun onNumpadClear() {
        currentInput.clear()
        updateDigitBoxes()
    }
    
    private fun updateDigitBoxes() {
        digitViews.forEachIndexed { index, textView ->
            if (index < currentInput.size) {
                textView.text = currentInput[index]
                textView.alpha = 1.0f
            } else {
                textView.text = ""
                textView.alpha = 0.5f
            }
        }
    }
    
    private fun getCurrentGuess(): String {
        return currentInput.joinToString("")
    }
    
    private fun shakeInput() {
        // Shake animation for all digit boxes
        digitViews.forEach { digitView ->
            val shake = ObjectAnimator.ofFloat(digitView, "translationX", 0f, -10f, 10f, -5f, 5f, 0f)
            shake.duration = 300
            shake.start()
        }
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
            
        applyThemeToDialogContent(dialogView)
        applyThemeToDialog(historyDialog!!, dialogView)
        historyDialog?.show()
    }
    
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
}

// Data class for guess results
data class GuessResult(
    val guess: String,
    val correctDigits: Int,
    val correctPositions: Int
)
