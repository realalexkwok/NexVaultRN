package expo.modules.securityandroid

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * T3.2 MVP Frida detection [C] (spec: security-android T-SA-7; demo subset = 1-2 signals).
 *
 * Signals: the default frida-server port (27042, plus the classic 27043 fallback) answering on
 * loopback, and frida-agent/frida-gadget strings in /proc/self/maps. Pure parsers over injected
 * inputs keep the JVM gate honest (TC-T32-002); [FridaDetector] only supplies the device inputs.
 *
 * [C]: Frida can be renamed and rebound to any port (frida-server -l), and a hooked maps read can
 * hide the strings. The full multi-signal scoring (fd/thread names, rwxp segments, inline-hook
 * prologue bytes) is the roadmap stage-2 work.
 */
internal object FridaPure {
  const val ID_MAPS = "FRIDA_MAPS"
  const val ID_PORT_PREFIX = "FRIDA_PORT_"

  val DEFAULT_PORTS = listOf(27042, 27043)
  private val MAPS_NEEDLES = listOf("frida-agent", "frida-gadget", "libfrida", "linjector")

  fun mapsIndicateFrida(mapsText: String): Boolean = MAPS_NEEDLES.any { mapsText.contains(it) }

  fun vectorIds(mapsText: String?, openPorts: Collection<Int>): List<String> {
    val ids = mutableListOf<String>()
    if (mapsText != null && mapsIndicateFrida(mapsText)) ids += ID_MAPS
    openPorts.sorted().forEach { port -> ids += ID_PORT_PREFIX + port }
    return ids
  }
}

internal object FridaDetector {
  private const val CONNECT_TIMEOUT_MS = 50

  fun evaluate(): List<String> {
    val maps = runCatching { File("/proc/self/maps").readText() }.getOrNull()
    val open = FridaPure.DEFAULT_PORTS.filter { portOpen(it) }
    return FridaPure.vectorIds(maps, open)
  }

  private fun portOpen(port: Int): Boolean = try {
    Socket().use { socket ->
      socket.connect(InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT_MS)
      true
    }
  } catch (e: Exception) {
    false
  }
}
