package expo.modules.securityandroid

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

/** Host activity for the biometric device demo (androidTest APK only; never shipped in the app). */
class TestBiometricActivity : FragmentActivity() {
  private lateinit var instructionView: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    instructionView = TextView(this).apply {
      textSize = 22f
      setTextColor(Color.WHITE)
      setBackgroundColor(Color.parseColor("#B00020"))
      setPadding(32, 48, 32, 48)
      gravity = Gravity.CENTER
    }
    val body = TextView(this).apply {
      text = "T3.1 biometric test host — follow the red banner"
      textSize = 16f
      setPadding(32, 32, 32, 32)
    }
    val layout = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      addView(instructionView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
      addView(body, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }
    setContentView(layout)
  }

  /** Updates the big banner; the instrumentation test calls this before each prompt. */
  fun showInstruction(text: String) {
    instructionView.text = text
  }
}
