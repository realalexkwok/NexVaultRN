import CommonCrypto
import Foundation

/// BIP-39 English validation and PBKDF2-HMAC-SHA512 seed derivation (2048 rounds) built on
/// CommonCrypto (owner gate-3: `CCKeyDerivationPBKDF`, no fallback, no third-party KDF).
///
/// The mnemonic arrives as UTF-8 bytes ([UInt8]) — never as a String from JS (R1). Every secret
/// scratch buffer is wiped with `wipe(_:)` in a `defer`. Derived `String`s and `Data` copies
/// cannot be wiped (CoW/heap) and are recorded as **[C]** caveats.
enum Bip39 {
  static let expectedWordCount = 2048
  static let allowedWordCounts: Set<Int> = [12, 15, 18, 21, 24]
  static let pbkdf2Rounds: UInt32 = 2048
  static let seedLength = 64

  /// Wordlist bundled with the module (`Resources/bip39_english.txt`, byte-identical to the
  /// Android `res/raw` copy; sha256 asserted in the XCTest suite). Fails closed when invalid.
  static func loadWordlist() -> [String] {
    // Anchor on a type from this (core) module: the Expo bridge class lives in the separate
    // VaultCore pod since T1.5-r2. In the app this resolves to the main bundle (static framework),
    // in the XCTest bundle to the test bundle — both carry `Resources/bip39_english.txt`.
    let bundle = Bundle(for: VaultStore.self)
    guard let url = bundle.url(forResource: "bip39_english", withExtension: "txt"),
          let data = try? Data(contentsOf: url),
          let content = String(data: data, encoding: .utf8) else {
      return []
    }
    let words = content
      .split(separator: "\n")
      .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
      .filter { !$0.isEmpty }
    return words.count == expectedWordCount ? words : []
  }

  /// Validates a NFKD-normalized mnemonic: word count, membership and checksum.
  static func validate(normalized: String, wordlist: [String]) -> Bool {
    guard wordlist.count == expectedWordCount else { return false }
    let words = normalized.split(whereSeparator: { $0.isWhitespace }).map(String.init)
    guard allowedWordCounts.contains(words.count) else { return false }

    let indexByWord = Dictionary(uniqueKeysWithValues: wordlist.enumerated().map { ($1, $0) })
    let totalBits = words.count * 11
    let entropyBits = totalBits * 32 / 33
    let checksumBits = totalBits - entropyBits
    let entropyByteCount = entropyBits / 8

    var bits = [UInt8]()
    bits.reserveCapacity(totalBits)
    for word in words {
      guard let index = indexByWord[word] else { return false }
      for shift in stride(from: 10, through: 0, by: -1) {
        bits.append(UInt8((index >> shift) & 1))
      }
    }

    var entropy = [UInt8](repeating: 0, count: entropyByteCount)
    defer {
      wipe(&entropy)
      wipe(&bits)
    }
    for bitIndex in 0..<entropyBits {
      if bits[bitIndex] == 1 {
        entropy[bitIndex / 8] |= UInt8(1 << (7 - (bitIndex % 8)))
      }
    }

    var digest = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
    defer { wipe(&digest) }
    CC_SHA256(entropy, CC_LONG(entropy.count), &digest)
    for bitIndex in 0..<checksumBits {
      let expected = (digest[bitIndex / 8] >> UInt8(7 - (bitIndex % 8))) & 1
      if expected != bits[entropyBits + bitIndex] { return false }
    }
    return true
  }

  /// PBKDF2-HMAC-SHA512 (2048 rounds) over the NFKD mnemonic; returns 64 bytes or nil.
  static func mnemonicToSeed(normalizedMnemonic: String, passphrase: String) -> [UInt8]? {
    var password = Array(normalizedMnemonic.utf8)
    var salt = Array(("mnemonic" + passphrase).utf8)
    defer {
      wipe(&password)
      wipe(&salt)
    }
    return pbkdf2SHA512(password: password, salt: salt, rounds: pbkdf2Rounds, keyLength: seedLength)
  }

  static func pbkdf2SHA512(password: [UInt8], salt: [UInt8], rounds: UInt32, keyLength: Int) -> [UInt8]? {
    var derived = [UInt8](repeating: 0, count: keyLength)
    let status = password.withUnsafeBufferPointer { passwordBuffer -> Int32 in
      salt.withUnsafeBufferPointer { saltBuffer -> Int32 in
        guard let passwordBase = passwordBuffer.baseAddress else { return Int32(kCCParamError) }
        let passwordPointer = UnsafeRawPointer(passwordBase).assumingMemoryBound(to: Int8.self)
        return CCKeyDerivationPBKDF(
          CCPBKDFAlgorithm(kCCPBKDF2),
          passwordPointer,
          passwordBuffer.count,
          saltBuffer.baseAddress,
          saltBuffer.count,
          CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA512),
          rounds,
          &derived,
          derived.count
        )
      }
    }
    guard status == kCCSuccess else {
      wipe(&derived)
      return nil
    }
    return derived
  }
}
