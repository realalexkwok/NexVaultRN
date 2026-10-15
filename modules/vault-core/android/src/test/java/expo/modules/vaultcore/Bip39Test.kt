package expo.modules.vaultcore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM automated gate for T1.2.
 *
 * V1–V6 are official BIP-0039 English vectors (TREZOR passphrase) mirrored by
 * https://github.com/trezor/python-mnemonic/blob/master/vectors.json (fetched 2026-10-14).
 * That file only ships 12/18/24-word rows, so V7 (15 words) and V8 (21 words) are derived
 * vectors whose entropy→mnemonic mapping and PBKDF2 seed were produced and independently
 * decoded with a separate Python implementation (evidence: validation.md, review-fix round
 * 2026-10-15). Seed assertions compare hex through the pure Kotlin core; the seed never
 * leaves native.
 *
 * Coverage (AC-2): positive vectors exist for all five accepted word counts
 * (12/15/18/21/24) plus the 11/13-word and checksum negatives.
 */
class Bip39Test {
  private data class Vector(
    val id: String,
    val entropyHex: String,
    val mnemonic: String,
    val seedHex: String
  )

  private val testWordlist: List<String> by lazy {
    val stream = javaClass.classLoader!!.getResourceAsStream("bip39_english.txt")
      ?: error("bip39_english.txt fixture missing from src/test/resources")
    stream.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.toList() }
  }

  private val vectors = listOf(
    Vector(
      id = "V1",
      entropyHex = "00000000000000000000000000000000",
      mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
      seedHex = "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04"
    ),
    Vector(
      id = "V2",
      entropyHex = "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f",
      mnemonic = "legal winner thank year wave sausage worth useful legal winner thank yellow",
      seedHex = "2e8905819b8723fe2c1d161860e5ee1830318dbf49a83bd451cfb8440c28bd6fa457fe1296106559a3c80937a1c1069be3a3a5bd381ee6260e8d9739fce1f607"
    ),
    Vector(
      id = "V3",
      entropyHex = "80808080808080808080808080808080",
      mnemonic = "letter advice cage absurd amount doctor acoustic avoid letter advice cage above",
      seedHex = "d71de856f81a8acc65e6fc851a38d4d7ec216fd0796d0a6827a3ad6ed5511a30fa280f12eb2e47ed2ac03b5c462a0358d18d69fe4f985ec81778c1b370b652a8"
    ),
    Vector(
      id = "V4",
      entropyHex = "ffffffffffffffffffffffffffffffff",
      mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
      seedHex = "ac27495480225222079d7be181583751e86f571027b0497b5b5d11218e0a8a13332572917f0f8e5a589620c6f15b11c61dee327651a14c34e18231052e48c069"
    ),
    // Official vectors.json row, 18 words.
    Vector(
      id = "V5",
      entropyHex = "000000000000000000000000000000000000000000000000",
      mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon agent",
      seedHex = "035895f2f481b1b0f01fcf8c289c794660b289981a78f8106447707fdd9666ca06da5a9a565181599b79f53b844d8a71dd9f439c52a3d7b3e8a79c906ac845fa"
    ),
    // Official vectors.json row, 24 words.
    Vector(
      id = "V6",
      entropyHex = "0000000000000000000000000000000000000000000000000000000000000000",
      mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art",
      seedHex = "bda85446c68413707090a52022edd26a1c9462295029f2e60cd7c4f2bbd3097170af7a4d73245cafa9c3cca8d561a7c3de6f5d4a10be8ed2a5e608d68f92fcc8"
    ),
    // Derived vector: 160-bit zero entropy (vectors.json has no 15-word row); the mnemonic
    // decodes back to this entropy with a correct checksum under an independent Python
    // BIP-39 implementation, and the seed below comes from Python PBKDF2-HMAC-SHA512.
    Vector(
      id = "V7",
      entropyHex = "0000000000000000000000000000000000000000",
      mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon address",
      seedHex = "fa08713f46bf5cb48728ceb70e3aae1bc53c5cb7b4e29c5610261d1cbb7be3bed4d805256fec515754d2be35974fc5da678168e9d9bb0cb70948026923b0def3"
    ),
    // Derived vector: 224-bit all-ones entropy (no 21-word row in vectors.json); same
    // independent cross-check as V7.
    Vector(
      id = "V8",
      entropyHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      mnemonic = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo veteran",
      seedHex = "4aa0af4ca02ef1d9fa675cd02aa06d318425564e7fadd3d51b6165cc56d77398f28d8522073cd036c2a4a24a83e919211c84500d96cb120084e613ff5fcd96c1"
    )
  )

  // TC-VC-001
  @Test
  fun vectorV1_abandonAbout() {
    assertVector(vectors[0])
  }

  // TC-VC-001
  @Test
  fun vectorV2_legalWinner() {
    assertVector(vectors[1])
  }

  // TC-VC-001
  @Test
  fun vectorV3_letterAdvice() {
    assertVector(vectors[2])
  }

  // TC-VC-001
  @Test
  fun vectorV4_zooWrong() {
    assertVector(vectors[3])
  }

  // TC-VC-001
  @Test
  fun vectorV5_abandonAgent() {
    assertVector(vectors[4])
  }

  // TC-VC-001
  @Test
  fun vectorV6_abandonArt() {
    assertVector(vectors[5])
  }

  // TC-VC-001
  @Test
  fun vectorV7_zeroEntropy15Words() {
    assertVector(vectors[6])
  }

  // TC-VC-001
  @Test
  fun vectorV8_onesEntropy21Words() {
    assertVector(vectors[7])
  }

  // TC-VC-001
  @Test
  fun fallbackLoopMatchesVectorV1() {
    val seedHex = Bip39.mnemonicToSeedUsingFallback(vectors[0].mnemonic, "TREZOR").toHex()
    assertEquals(vectors[0].seedHex, seedHex)
  }

  // TC-VC-001
  @Test
  fun emptyPassphraseDerivesDistinctVectorSeed() {
    val expectedEmptyPassphraseSeed =
      "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1" +
        "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4"
    val actual = Bip39.mnemonicToSeed(vectors[0].mnemonic, "").toHex()
    assertEquals(expectedEmptyPassphraseSeed, actual)
    assertNotEquals(vectors[0].seedHex, actual)
  }

  // TC-VC-002
  @Test
  fun rejectsUnknownWord() {
    val corrupted = vectors[0].mnemonic.replace("about", "zzzz")
    assertFalse(Bip39.validateMnemonic(corrupted, testWordlist))
  }

  // TC-VC-002
  @Test
  fun rejectsChecksumMismatch() {
    // All words are in the list; only the checksum changes.
    val corrupted = vectors[0].mnemonic.replace("about", "abandon")
    assertFalse(Bip39.validateMnemonic(corrupted, testWordlist))
  }

  // TC-VC-002
  @Test
  fun rejectsWrongWordCount() {
    val elevenWords = vectors[0].mnemonic.substringBeforeLast(" ")
    val thirteenWords = "${vectors[0].mnemonic} abandon"
    assertFalse(Bip39.validateMnemonic(elevenWords, testWordlist))
    assertFalse(Bip39.validateMnemonic(thirteenWords, testWordlist))
  }

  // TC-VC-010
  @Test
  fun wordlistHasExactly2048Words() {
    assertEquals(2048, testWordlist.size)
    assertEquals(testWordlist.sorted(), testWordlist)
  }

  // TC-VC-010
  @Test
  fun emptyWordlistFailsClosed() {
    assertFalse(Bip39.validateMnemonic(vectors[0].mnemonic, emptyList()))
  }

  private fun assertVector(vector: Vector) {
    assertTrue(
      "${vector.id} mnemonic should validate",
      Bip39.validateMnemonic(vector.mnemonic, testWordlist)
    )
    assertEquals(
      "${vector.id} TREZOR seed",
      vector.seedHex,
      Bip39.mnemonicToSeed(vector.mnemonic, "TREZOR").toHex()
    )
    // Entropy stays a fixture-only field (T1.2 never exposes entropy bytes to JS), but it
    // must stay self-consistent with the mnemonic: BIP-39 entropy bits = words * 32 / 3
    // (12→128, 15→160, 18→192, 21→224, 24→256), i.e. words * 32 / 3 == hexChars * 4.
    val wordCount = vector.mnemonic.split(" ").size
    assertEquals(
      "${vector.id} entropy bits must match its word count",
      wordCount * 32 / 3,
      vector.entropyHex.length * 4
    )
    assertTrue(
      "${vector.id} entropy must be lowercase hex",
      vector.entropyHex.matches(Regex("[0-9a-f]+"))
    )
  }

  private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte.toInt() and 0xFF)
  }
}
