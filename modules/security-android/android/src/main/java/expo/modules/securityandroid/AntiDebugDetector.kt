package expo.modules.securityandroid

import java.io.File

/**
 * T3.2 MVP anti-debug detection [C] (spec: security-android T-SA-6; demo subset = TracerPid).
 *
 * Reads /proc/self/status and parses TracerPid; any value > 0 means a debugger is attached. The
 * parser is a pure function over injected text (JVM gate, TC-T32-003). The TRACEME self-attach and
 * task-stat combination from the spec stays a roadmap item (it needs a ptrace call from a second
 * thread and is easy to false-positive).
 *
 * [C]: on a rooted device the status read can be intercepted; this only raises the cost.
 */
internal object AntiDebugPure {
  const val ID_TRACERPID = "DBG_TRACERPID"
  private const val FIELD = "TracerPid:"

  /** Returns the TracerPid value, or -1 when the field is absent or malformed. */
  fun parseTracerPid(statusText: String): Int {
    for (line in statusText.lineSequence()) {
      if (!line.startsWith(FIELD)) continue
      return line.substringAfter(':').trim().toIntOrNull() ?: -1
    }
    return -1
  }

  fun isTraced(statusText: String): Boolean = parseTracerPid(statusText) > 0
}

internal object AntiDebugDetector {
  fun evaluate(): Boolean {
    val status = runCatching { File("/proc/self/status").readText() }.getOrNull() ?: return false
    return AntiDebugPure.isTraced(status)
  }
}
