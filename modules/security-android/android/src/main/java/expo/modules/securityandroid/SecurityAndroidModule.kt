package expo.modules.securityandroid

import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal JS face for T3.1 (requirements FR-4): getCapabilities / ensureWrapKey / deleteWrapKey
 * only. wrap/unwrap are native-internal today; T3.2 wires them to vault-core together with the
 * E_VAULT_SECURITY_GATE fail-closed coupling.
 */
class SecurityAndroidModule : Module() {
  private val manager: WrapKeyManager by lazy {
    WrapKeyManager(requireNotNull(appContext.reactContext) { "React context is unavailable" })
  }

  override fun definition() = ModuleDefinition {
    Name("SecurityAndroid")

    AsyncFunction("getCapabilities") Coroutine { ->
      withContext(Dispatchers.Default) {
        mapOf(
          "hasStrongBox" to manager.hasStrongBox(),
          "isDeviceSecure" to manager.isDeviceSecure(),
          "securityLevel" to manager.existingSecurityLevel()?.name
        )
      }
    }

    AsyncFunction("ensureWrapKey") Coroutine { ->
      withContext(Dispatchers.Default) {
        val info = manager.ensureKey()
        mapOf(
          "alias" to info.alias,
          "securityLevel" to info.securityLevel.name,
          "strongBoxBacked" to info.strongBoxBacked
        )
      }
    }

    AsyncFunction("deleteWrapKey") Coroutine { ->
      withContext(Dispatchers.Default) {
        manager.deleteKey()
        Unit
      }
    }
  }
}
