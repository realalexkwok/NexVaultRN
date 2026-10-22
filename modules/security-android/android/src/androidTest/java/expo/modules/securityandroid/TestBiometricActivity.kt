package expo.modules.securityandroid

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

/** Host activity for the biometric device demo (androidTest APK only; never shipped in the app). */
class TestBiometricActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(TextView(this).apply { text = "T3.1 biometric test host" })
  }
}
