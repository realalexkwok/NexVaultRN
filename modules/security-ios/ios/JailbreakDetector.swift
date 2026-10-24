import Darwin
import Foundation

/**
 * Self-written jailbreak detection vectors (T3.2, D1 2026-10-24; GATE-R4-IOSSECURITYSUITE was
 * adjudicated to zero new dependencies — IOSSecuritySuite is NOT used).
 *
 * Every signal here is a cost-raiser only: on a jailbroken device with hidejb-class hooks these
 * checks are bypassable by a patient attacker, so the module labels all of them [C] and never
 * claims [P] on their behalf (AGENTS.md R6; spec: security-ios section 3, T-SI-3).
 *
 * Reports carry stable vector IDs only — never plaintext paths, library names or other detail
 * (AGENTS.md R5). The pure halves take injected inputs, so the Mac XCTest suite exercises
 * positives and negatives without a jailbroken device; the device wrappers just feed real values.
 */
enum JailbreakDetector {
  /// Rootless (Dopamine) family + classic artefacts in a fixed order, so reports are deterministic.
  static let pathVectors: [(id: String, path: String)] = [
    ("JB_PATH_ROOTLESS", "/var/jb"),
    ("JB_PATH_CYDIA", "/Applications/Cydia.app"),
    ("JB_PATH_SSHD", "/usr/sbin/sshd"),
    ("JB_PATH_SUBSTRATE", "/Library/MobileSubstrate"),
    ("JB_PATH_APT", "/etc/apt"),
  ]

  /// Lower-cased markers for injected instrumentation (Frida gadget) and hooking frameworks.
  static let fridaMarkers = ["fridagadget", "frida-agent", "frida"]
  static let hookMarkers = ["systemhook", "ellekit", "shadow", "substitute", "tweakinject", "libhooker"]

  /// Pure aggregation used by both the device wrappers and the fixtures (TC-SI-004 / T3.2 tests).
  static func collectSignals(
    pathExists: (String) -> Bool,
    loadedImages: [String],
    bootTimeAnomaly: Bool
  ) -> [String] {
    var signals = [String]()
    for vector in pathVectors where pathExists(vector.path) {
      signals.append(vector.id)
    }
    signals.append(contentsOf: collectDyldSignals(loadedImages: loadedImages))
    if bootTimeAnomaly {
      signals.append("JB_BOOTTIME")
    }
    return signals
  }

  /// Pure dyld scan: stable IDs, deduplicated, first-seen order.
  static func collectDyldSignals(loadedImages: [String]) -> [String] {
    var signals = [String]()
    for image in loadedImages {
      let lower = image.lowercased()
      if fridaMarkers.contains(where: { lower.contains($0) }) {
        appendUnique(&signals, "JB_DYLD_FRIDA")
      }
      if hookMarkers.contains(where: { lower.contains($0) }) {
        appendUnique(&signals, "JB_DYLD_HOOK")
      }
    }
    return signals
  }

  /// Pure boot-time vector: device boot time vs (now - uptime); >5 min divergence means tampering.
  static func bootTimeAnomaly(bootTimeSeconds: Int64, now: TimeInterval, uptime: TimeInterval) -> Bool {
    abs((now - uptime) - Double(bootTimeSeconds)) > 300
  }

  // MARK: - Device-facing wrappers

  static func pathFamilySignals() -> [String] {
    let fileManager = FileManager.default
    return collectSignals(
      pathExists: { fileManager.fileExists(atPath: $0) },
      loadedImages: [],
      bootTimeAnomaly: false
    )
  }

  /// Scans the loaded Mach-O images for injected instrumentation/hooking libraries.
  static func dyldSignals() -> [String] {
    var images = [String]()
    let count = _dyld_image_count()
    for index in 0..<count {
      if let name = _dyld_get_image_name(index) {
        images.append(String(cString: name))
      }
    }
    return collectDyldSignals(loadedImages: images)
  }

  /// sysctl(KERN_PROC_PID) P_TRACED — a debugger is attached to this process.
  static func isDebuggerAttached() -> Bool {
    var info = kinfo_proc()
    var size = MemoryLayout<kinfo_proc>.stride
    var mib: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
    guard sysctl(&mib, u_int(mib.count), &info, &size, nil, 0) == 0 else { return false }
    return (info.kp_proc.p_flag & P_TRACED) != 0
  }

  /// Best-effort PT_DENY_ATTACH through dlsym. PT_DENY_ATTACH is not public API and the binary
  /// must never link ptrace directly (App Store review risk; spec section 2.3 / TC-SI-005 grep gate).
  static func denyDebuggerAttach() {
    typealias PtraceFunction = @convention(c) (Int32, pid_t, UnsafeMutableRawPointer?, Int32) -> Int32
    // RTLD_DEFAULT == (void *)-2; resolving at runtime keeps the symbol out of the link table.
    guard let symbol = dlsym(UnsafeMutableRawPointer(bitPattern: -2), "ptrace") else { return }
    let call = unsafeBitCast(symbol, to: PtraceFunction.self)
    _ = call(31 /* PT_DENY_ATTACH */, 0, nil, 0)
  }

  /// sysctl(KERN_BOOTTIME) compared against ProcessInfo.systemUptime.
  static func isBootTimeAnomalous() -> Bool {
    var bootTime = timeval()
    var size = MemoryLayout<timeval>.stride
    var mib: [Int32] = [CTL_KERN, KERN_BOOTTIME]
    guard sysctl(&mib, u_int(mib.count), &bootTime, &size, nil, 0) == 0 else { return false }
    return bootTimeAnomaly(
      bootTimeSeconds: Int64(bootTime.tv_sec),
      now: Date().timeIntervalSince1970,
      uptime: ProcessInfo.processInfo.systemUptime
    )
  }

  private static func appendUnique(_ signals: inout [String], _ signal: String) {
    if !signals.contains(signal) {
      signals.append(signal)
    }
  }
}
