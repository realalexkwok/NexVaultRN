package expo.modules.vaultcore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM automated gate for T1.2.
 *
 * V1–V4 are the first four official BIP-0039 English vectors (TREZOR passphrase),
 * mirrored by https://github.com/trezor/python-mnemonic/blob/master/vectors.json
 * (fetched 2026-10-14). Seed assertions compare hex through the pure Kotlin core;
 * the seed never leaves native.
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
    // Entropy is embedded in the vector fixture to keep the official id visible,
    // but T1.2 does not expose entropy bytes to JS.
    assertEquals(32, vector.entropyHex.length)
  }

  private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte.toInt() and 0xFF)
  }
}
