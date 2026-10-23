package expo.modules.securityandroid

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import expo.modules.kotlin.exception.CodedException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.UnrecoverableKeyException
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal enum class SecurityLevel { STRONGBOX, TRUSTED_ENVIRONMENT, SOFTWARE_DEBUG }

internal data class WrapKeyInfo(
  val alias: String,
  val securityLevel: SecurityLevel,
  val strongBoxBacked: Boolean
)

internal data class WrapCapabilities(
  val hasStrongBox: Boolean,
  val isDeviceSecure: Boolean,
  val securityLevel: SecurityLevel?
)

/**
 * T3.1 wrap-key manager (spec: security-android sections 2.1 / 2.4).
 *
 * The AES-256-GCM key lives in the Android Keystore and is created with the spec's
 * user-authentication flag set: per-use window 0, enrollment invalidation, StrongBox with a TEE
 * retry, and unlockedDeviceRequired only on API 35+ (documented 28-34 bug). wrap/unwrap stay
 * native-internal; only the instrumentation suite drives them today and T3.2 wires them to
 * vault-core behind the minimal JS face.
 *
 * Provenance of the security level (device finding 2026-10-22): keystore2 exposes KeyInfo only for
 * private keys, so an AES key's hardware backing cannot be queried after the fact. The level is
 * therefore the generation path's provenance, cached in memory: a successful StrongBox attempt ->
 * STRONGBOX; the TEE path or a later process re-open -> TRUSTED_ENVIRONMENT. TRUSTED_ENVIRONMENT
 * is an **unverified floor** for non-emulator devices, not an attestation claim: a device whose
 * Keystore is software-backed would be labelled TRUSTED_ENVIRONMENT (owner decision D9, open);
 * verification belongs to the T5.x attestation work. Emulator -> SOFTWARE_DEBUG, refused on a
 * non-debuggable build. No level marker is persisted (R5's on-disk whitelist is addresses and
 * SecureStore/Keychain ciphertext only).
 *
 * Honest boundary: the Keystore and BiometricPrompt paths cannot run on the JVM; the pure blob
 * codec lives in [AesGcmEnvelope] and carries the JVM gate.
 */
internal class WrapKeyManager(private val context: Context) {
  companion object {
    const val ALIAS = "nexvault.wrap.v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val MAX_FAILED_ATTEMPTS = 3
    private const val PROMPT_TIMEOUT_MS = 180_000L
  }

  /** In-memory provenance of the key created by this process (never persisted; see class KDoc). */
  private var generatedLevel: SecurityLevel? = null

  fun hasStrongBox(): Boolean =
    Build.VERSION.SDK_INT >= 28 &&
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

  fun isDeviceSecure(): Boolean =
    (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true

  /** Security level of the existing wrap key, or null when no key has been created yet. */
  fun existingSecurityLevel(): SecurityLevel? {
    if (!keyStore().containsAlias(ALIAS)) return null
    return observedLevel()
  }

  /**
   * Creates the wrap key when missing and reports its provenance level. Never degrades to a
   * software key silently: a SOFTWARE_DEBUG level on a non-debuggable build raises
   * E_SEC_SOFTWARE_KEYSTORE (spec section 2.5).
   */
  fun ensureKey(): WrapKeyInfo {
    if (!isDeviceSecure()) throw NoLockscreenException()
    val store = keyStore()
    if (!store.containsAlias(ALIAS)) {
      generatedLevel = generateKey()
    }
    val info = infoFor(observedLevel())
    if (info.securityLevel == SecurityLevel.SOFTWARE_DEBUG && !isDebuggable()) {
      throw SoftwareKeystoreException()
    }
    return info
  }

  /** Idempotent removal of the wrap key (missing alias is a no-op). */
  fun deleteKey() {
    try {
      keyStore().deleteEntry(ALIAS)
      generatedLevel = null
    } catch (e: KeyStoreException) {
      throw KeyInvalidatedException()
    }
  }

  /** ENCRYPT cipher for the wrap path; the caller binds it to a BiometricPrompt CryptoObject. */
  internal fun encryptCipher(): Cipher {
    val key = loadKey()
    return try {
      Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    } catch (e: KeyPermanentlyInvalidatedException) {
      throw KeyInvalidatedException()
    }
  }

  /** DECRYPT cipher for the unwrap path; the blob is validated before anything prompts. */
  internal fun decryptCipher(blob: ByteArray): Pair<Cipher, AesGcmEnvelope.Parsed> {
    val parsed = AesGcmEnvelope.parse(blob)
    val key = loadKey()
    val cipher = try {
      Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AesGcmEnvelope.TAG_BITS, parsed.iv))
      }
    } catch (e: KeyPermanentlyInvalidatedException) {
      throw KeyInvalidatedException()
    }
    return cipher to parsed
  }

  /**
   * Wraps [plaintext] behind a CryptoObject-bound BiometricPrompt. The input buffer is wiped on
   * every path — including a cancelled or failed biometric gate — by the outer finally (AC-7/R1);
   * the returned blob is safe to persist (ciphertext only).
   */
  suspend fun wrap(activity: FragmentActivity, plaintext: ByteArray, aad: ByteArray): ByteArray {
    try {
      val cipher = encryptCipher()
      val authorized = authenticate(activity, cipher)
      val aadCopy = aad.copyOf()
      try {
        return AesGcmEnvelope.seal(authorized, plaintext, aadCopy)
      } finally {
        aadCopy.fill(0)
      }
    } finally {
      // A second fill after a successful seal is an idempotent belt-and-braces, not a copy.
      plaintext.fill(0)
    }
  }

  /**
   * Unwraps [blob] behind a CryptoObject-bound BiometricPrompt. The returned plaintext is owned by
   * the caller (the native signing window in T3.2) and must be wiped there.
   */
  suspend fun unwrap(activity: FragmentActivity, blob: ByteArray, aad: ByteArray): ByteArray {
    val (cipher, parsed) = decryptCipher(blob)
    val authorized = authenticate(activity, cipher)
    val aadCopy = aad.copyOf()
    try {
      return AesGcmEnvelope.open(authorized, parsed, aadCopy)
    } finally {
      aadCopy.fill(0)
    }
  }

  /**
   * Generation path provenance for the auth-bound wrap key.
   *
   * 2026-10-22 device finding (Pixel 6a / Android 17): a StrongBox-backed auth-per-use AES key is
   * created successfully, but every authenticated use fails inside the Titan M2 KeyMint instance
   * (citadel) with KEY_USER_NOT_AUTHENTICATED -- StrongBox requires a hardware auth token for
   * per-use keys, and the biometric prompt's token is not accepted there. The wrap key therefore
   * takes the TEE path; StrongBox support is a T5.x investigation (recorded Handoff).
   */
  private fun generateKey(): SecurityLevel {
    if (isEmulator()) {
      generateWithSpec(buildSpec(strongBox = false))
      return SecurityLevel.SOFTWARE_DEBUG
    }
    generateWithSpec(buildSpec(strongBox = false))
    return SecurityLevel.TRUSTED_ENVIRONMENT
  }

  private fun generateWithSpec(spec: KeyGenParameterSpec) {
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
    generator.init(spec)
    generator.generateKey()
  }

  private fun buildSpec(strongBox: Boolean): KeyGenParameterSpec {
    val builder = KeyGenParameterSpec.Builder(
      ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setUserAuthenticationRequired(true)
      .setInvalidatedByBiometricEnrollment(true)

    if (strongBox && Build.VERSION.SDK_INT >= 28) {
      builder.setIsStrongBoxBacked(true)
    }
    if (Build.VERSION.SDK_INT >= 30) {
      // D6 (owner 2026-10-22): biometric-only. The per-use CryptoObject flow cannot be authorised
      // by the device credential (androidx 1.1.0 delivers a null crypto object for a correct PIN),
      // so advertising the PIN fallback would be a dead path; per-use BIOMETRIC_STRONG is the
      // strongest binding and matches the demo gate.
      builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
    }
    if (Build.VERSION.SDK_INT >= 35) {
      builder.setUnlockedDeviceRequired(true)
    }
    return builder.build()
  }

  private fun observedLevel(): SecurityLevel = generatedLevel ?: if (isEmulator()) {
    SecurityLevel.SOFTWARE_DEBUG
  } else {
    SecurityLevel.TRUSTED_ENVIRONMENT
  }

  private fun infoFor(level: SecurityLevel): WrapKeyInfo =
    WrapKeyInfo(ALIAS, level, level == SecurityLevel.STRONGBOX)

  private fun loadKey(): SecretKey {
    val store = keyStore()
    if (!store.containsAlias(ALIAS)) throw KeyInvalidatedException()
    return try {
      store.getKey(ALIAS, null) as? SecretKey ?: throw KeyInvalidatedException()
    } catch (e: UnrecoverableKeyException) {
      throw KeyInvalidatedException()
    }
  }

  private fun isDebuggable(): Boolean =
    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

  private fun isEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT
    return fingerprint.startsWith("generic") ||
      fingerprint.contains("emulator") ||
      Build.MODEL.contains("sdk_gphone") ||
      Build.HARDWARE.contains("goldfish") ||
      Build.HARDWARE.contains("ranchu")
  }

  private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

  private suspend fun authenticate(activity: FragmentActivity, cipher: Cipher): Cipher {
    val info = try {
      promptInfo()
    } catch (e: IllegalArgumentException) {
      throw SecurityUnsupportedException()
    }

    // withTimeoutOrNull only returns null for this call's own timeout; an outer caller's
    // cancellation propagates as structured cancellation instead of being mis-labelled.
    val authorized = withTimeoutOrNull(PROMPT_TIMEOUT_MS) {
      suspendCancellableCoroutine { continuation ->
        val failureGate = PromptFailureGate(MAX_FAILED_ATTEMPTS)
        val settled = AtomicBoolean(false)
        var prompt: BiometricPrompt? = null

        fun finishWith(exception: CodedException) {
          if (settled.compareAndSet(false, true) && continuation.isActive) {
            continuation.resumeWithException(exception)
          }
        }

        fun finishWithCipher(authorizedCipher: Cipher?) {
          if (settled.compareAndSet(false, true) && continuation.isActive) {
            if (authorizedCipher == null) {
              continuation.resumeWithException(BiometricCancelledException())
            } else {
              continuation.resume(authorizedCipher)
            }
          }
        }

        // BiometricPrompt/FragmentManager are main-thread objects: every touch is posted.
        fun cancelPrompt() {
          activity.runOnUiThread {
            try {
              prompt?.cancelAuthentication()
            } catch (ignored: Throwable) {
              // The prompt may already be gone; cancellation must never crash the caller.
            }
          }
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            finishWithCipher(result.cryptoObject?.cipher)
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            finishWith(mapError(errorCode))
          }

          override fun onAuthenticationFailed() {
            // Demo gate (owner 2026-10-22): three wrong fingerprints fail closed instead of
            // waiting for the system lockout at five attempts. The counter is JVM-tested.
            if (failureGate.onFailure()) {
              finishWith(BiometricCancelledException())
              cancelPrompt()
            }
          }
        }

        continuation.invokeOnCancellation { cancelPrompt() }

        activity.runOnUiThread {
          // The timeout may have settled this attempt before the post runs: never create a prompt
          // for an attempt the caller has already abandoned.
          if (settled.get() || !continuation.isActive) return@runOnUiThread
          if (activity.isFinishing || activity.isDestroyed || activity.supportFragmentManager.isStateSaved) {
            finishWith(SecurityUnsupportedException())
            return@runOnUiThread
          }
          try {
            val created = BiometricPrompt(
              activity,
              ContextCompat.getMainExecutor(activity),
              callback
            )
            prompt = created
            if (!settled.get() && continuation.isActive) {
              created.authenticate(info, BiometricPrompt.CryptoObject(cipher))
            }
          } catch (e: Throwable) {
            finishWith(SecurityUnsupportedException())
          }
        }
      }
    }

    return authorized ?: throw BiometricCancelledException()
  }

  private fun promptInfo(): BiometricPrompt.PromptInfo {
    // D6 (owner 2026-10-22): biometric-only on every API level; the negative button is always
    // required when no device-credential fallback is allowed.
    return BiometricPrompt.PromptInfo.Builder()
      .setTitle("Unlock wallet key")
      .setSubtitle("Confirm it is you")
      .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
      .setNegativeButtonText("Cancel")
      .setConfirmationRequired(false)
      .build()
  }

  private fun mapError(errorCode: Int): CodedException = when (errorCode) {
    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricLockoutException()
    else -> BiometricCancelledException()
  }
}
