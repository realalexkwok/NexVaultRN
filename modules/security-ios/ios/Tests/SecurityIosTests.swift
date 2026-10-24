import CryptoKit
import XCTest
@testable import SecurityIos

/**
 * TC-SI-010 / TC-SI-011 (Mac-executed): blob structure parity with the Android vector set, the
 * codec's fail-closed paths, and the KEK coordinate contract against the installed
 * expo-secure-store 57.0.4. The biometric KEK read is device-gated and covered by MS-4 in
 * MAC-HANDOFF.md, not here.
 *
 * T3.2 additions (2026-10-24, status [~] — authored on Linux, never compiled): TC-SI-009 (KEK
 * lifecycle on test-only coordinates), JailbreakDetector fixtures (no jailbroken device needed),
 * and the fail-closed gate decision tests.
 */
final class SecurityIosTests: XCTestCase {
  private let aad = Data("0x9858EfFD232B4033E47d90003D41EC34EcaEda94|demo-user".utf8)

  private func plaintext(_ count: Int = 32) -> Data {
    Data((0..<count).map { UInt8(($0 + 1) % 256) })
  }

  // TC-SI-010
  func testBlobLayoutMatchesAndroid() throws {
    let key = SymmetricKey(size: .bits256)
    let iv = Data((0..<12).map { UInt8($0 + 1) })
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad, iv: iv)

    XCTAssertEqual(blob.count, 1 + 12 + 32 + 16)
    XCTAssertEqual(blob.first, AesGcmEnvelope.version)

    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertEqual(parsed.iv, iv)
    XCTAssertEqual(parsed.ciphertext.count, 32 + 16)
  }

  // TC-SI-010
  func testRoundtripReturnsTheOriginalPlaintext() throws {
    let key = SymmetricKey(size: .bits256)
    let input = plaintext(48)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: input, aad: aad)
    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertEqual(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad), input)
  }

  // TC-SI-010
  func testWrongAadFailsClosed() throws {
    let key = SymmetricKey(size: .bits256)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    let parsed = try AesGcmEnvelope.parse(blob)
    let wrongAad = Data("0x9858EfFD232B4033E47d90003D41EC34EcaEda94|another-user".utf8)
    XCTAssertThrowsError(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: wrongAad)) { error in
      XCTAssertEqual(error as? SecurityIosError, .blobCorrupt)
    }
  }

  // TC-SI-010
  func testTamperedTagAndVersionFailClosed() throws {
    let key = SymmetricKey(size: .bits256)
    var blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    blob[blob.count - 1] ^= 0x80
    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertThrowsError(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad))

    var wrongVersion = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    wrongVersion[0] = 2
    XCTAssertThrowsError(try AesGcmEnvelope.parse(wrongVersion))
  }

  // TC-SI-010
  func testTruncatedBlobFailsClosed() throws {
    let key = SymmetricKey(size: .bits256)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    XCTAssertThrowsError(try AesGcmEnvelope.parse(blob.prefix(1 + 12 + 15)))
    XCTAssertThrowsError(try AesGcmEnvelope.parse(Data()))
  }

  // N7 parity: the zero-length plaintext blob (29 bytes) is valid on both platforms.
  func testEmptyPlaintextBlobIsValid() throws {
    let key = SymmetricKey(size: .bits256)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: Data(), aad: aad)
    XCTAssertEqual(blob.count, 1 + 12 + 16)
    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertEqual(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad), Data())
  }

  // TC-SI-011 (MS-3): the KEK coordinate contract, asserted without seeding the production item.
  func testKekCoordinatesMatchExpoSecureStore() throws {
    // Literal regression guard: if KeychainKek.service/account drift, this fails first.
    XCTAssertEqual(KeychainKek.service, "app:auth")
    XCTAssertEqual(KeychainKek.account, "nexvault.kek.v1")

    // Seed a TEST-ONLY coordinate (never the production KEK item) with the same encoding
    // expo-secure-store uses (kSecAttrGeneric/kSecAttrAccount = Data(key.utf8), hex string value)
    // and read it back through the injected coordinates: verifies the Data-account encoding and
    // hex decoding.
    let testService = "app:auth.t3-1-test"
    let testAccount = "nexvault.kek.v1.t3-1-test"
    let accountData = Data(testAccount.utf8)
    let kekHex = (0..<32).map { String(format: "%02x", $0) }.joined()
    let item: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: testService,
      kSecAttrGeneric as String: accountData,
      kSecAttrAccount as String: accountData,
      kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
      kSecValueData as String: Data(kekHex.utf8)
    ]
    let deleteQuery: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: testService,
      kSecAttrGeneric as String: accountData,
      kSecAttrAccount as String: accountData
    ]
    SecItemDelete(deleteQuery as CFDictionary)
    XCTAssertEqual(SecItemAdd(item as CFDictionary, nil), errSecSuccess)
    defer { SecItemDelete(deleteQuery as CFDictionary) }

    let kek = try KeychainKek.loadKek(service: testService, account: testAccount)
    let bytes = kek.withUnsafeBytes { Data($0) }
    XCTAssertEqual(bytes, Data((0..<32).map { UInt8($0) }))
  }

  // MARK: - T3.2 additions ([~] authored, not yet compiled)

  /// TC-SI-009: full KEK lifecycle on TEST-ONLY coordinates — ensure (seed) -> wrap -> unwrap ->
  /// delete -> E_SEC_IOS_KEK_MISSING. The JS ensureKek/deleteKek leg and the biometric read run on a
  /// device (MS-4); this asserts the native half that the JS face drives.
  func testKekLifecycleEnsureWrapUnwrapDeleteTCSI009() throws {
    let testService = "app:auth.t3-2-test"
    let testAccount = "nexvault.kek.v1.t3-2-test"
    let accountData = Data(testAccount.utf8)
    let kekHex = (0..<32).map { String(format: "%02x", $0) }.joined()
    let item: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: testService,
      kSecAttrGeneric as String: accountData,
      kSecAttrAccount as String: accountData,
      kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
      kSecValueData as String: Data(kekHex.utf8)
    ]
    let deleteQuery: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: testService,
      kSecAttrGeneric as String: accountData,
      kSecAttrAccount as String: accountData
    ]
    SecItemDelete(deleteQuery as CFDictionary)
    XCTAssertEqual(SecItemAdd(item as CFDictionary, nil), errSecSuccess)
    defer { SecItemDelete(deleteQuery as CFDictionary) }

    // ensure: the entry resolves to a 32-byte KEK (same encoding expo-secure-store writes).
    let key = try KeychainKek.loadKek(service: testService, account: testAccount)
    XCTAssertEqual(key.withUnsafeBytes { Data($0) }.count, KeychainKek.kekSize)

    // wrap -> unwrap roundtrip through the shared envelope.
    let input = plaintext(32)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: input, aad: aad)
    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertEqual(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad), input)

    // delete -> the KEK is gone and the read fails closed (no plaintext output).
    XCTAssertEqual(SecItemDelete(deleteQuery as CFDictionary), errSecSuccess)
    XCTAssertThrowsError(try KeychainKek.loadKek(service: testService, account: testAccount)) { error in
      XCTAssertEqual(error as? SecurityIosError, .kekMissing)
    }
  }

  // JailbreakDetector fixtures (spec TC-SI-004 native half; no jailbroken device required).
  func testJailbreakPathVectorsUseStableIds() {
    let signals = JailbreakDetector.collectSignals(
      pathExists: { $0 == "/var/jb" || $0 == "/Applications/Cydia.app" },
      loadedImages: [],
      bootTimeAnomaly: false
    )
    XCTAssertEqual(signals, ["JB_PATH_ROOTLESS", "JB_PATH_CYDIA"])
  }

  func testCleanHostFixtureYieldsNoSignals() {
    let signals = JailbreakDetector.collectSignals(
      pathExists: { _ in false },
      loadedImages: ["/usr/lib/libSystem.B.dylib", "/System/Library/Frameworks/UIKit.framework/UIKit"],
      bootTimeAnomaly: false
    )
    XCTAssertEqual(signals, [])
  }

  func testDyldFixturesMapToStableIds() {
    XCTAssertEqual(
      JailbreakDetector.collectDyldSignals(loadedImages: ["/usr/lib/FridaGadget.dylib"]),
      ["JB_DYLD_FRIDA"]
    )
    XCTAssertEqual(
      JailbreakDetector.collectDyldSignals(loadedImages: ["/var/jb/usr/lib/libsystemhook.dylib"]),
      ["JB_DYLD_HOOK"]
    )
    XCTAssertEqual(
      JailbreakDetector.collectDyldSignals(loadedImages: ["/usr/lib/libSystem.B.dylib"]),
      []
    )
  }

  func testBootTimeAnomalyFixture() {
    XCTAssertTrue(JailbreakDetector.bootTimeAnomaly(bootTimeSeconds: 1_000_000, now: 1_000_000, uptime: 5_000))
    XCTAssertFalse(JailbreakDetector.bootTimeAnomaly(bootTimeSeconds: 995_000, now: 1_000_000, uptime: 5_000))
    XCTAssertEqual(
      JailbreakDetector.collectSignals(pathExists: { _ in false }, loadedImages: [], bootTimeAnomaly: true),
      ["JB_BOOTTIME"]
    )
  }

  // Fail-closed gate fixtures. The [P] coupling is device-verified on Android only; here we prove
  // the decision function and the thrown code.
  func testGateDeniesWhenReportHasSignals() {
    SecurityState.resetForTest()
    defer { SecurityState.resetForTest() }
    let report = DetectionReport(
      jailbreakSignals: ["JB_PATH_ROOTLESS"],
      fridaSignals: [],
      debuggerAttached: false,
      evaluatedAt: Date()
    )
    SecurityState.debugApplyForTest(report)
    XCTAssertEqual(DetectionPure.evaluate(report), .l4)
    XCTAssertTrue(DetectionPure.isDenied(.l4))
    XCTAssertEqual(SecurityGateDenied.code, "E_SEC_GATE_DENIED")
    XCTAssertThrowsError(try SecurityGate.assertSigningAllowed()) { error in
      XCTAssertTrue(error is SecurityGateDenied)
    }
  }

  func testGateAllowsCleanReport() throws {
    SecurityState.resetForTest()
    defer { SecurityState.resetForTest() }
    let report = DetectionReport(jailbreakSignals: [], fridaSignals: [], debuggerAttached: false, evaluatedAt: Date())
    SecurityState.debugApplyForTest(report)
    XCTAssertEqual(DetectionPure.evaluate(report), .l0)
    XCTAssertFalse(DetectionPure.isDenied(.l0))
    XCTAssertNoThrow(try SecurityGate.assertSigningAllowed())
  }

  func testGateRefreshesCacheWhenNoReportExists() {
    SecurityState.resetForTest()
    defer { SecurityState.resetForTest() }
    XCTAssertNil(SecurityState.cachedReport())
    // Clean hosts pass; if a real signal fires, the throw is the correct fail-closed behaviour.
    _ = try? SecurityGate.assertSigningAllowed()
    XCTAssertNotNil(SecurityState.cachedReport())
    if let cached = SecurityState.cachedReport() {
      XCTAssertEqual(SecurityState.cachedRiskLevel(), DetectionPure.evaluate(cached))
    }
  }
}
