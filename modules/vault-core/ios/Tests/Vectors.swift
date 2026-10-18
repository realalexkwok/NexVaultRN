import Foundation

/// Oracle vectors transcribed mechanically from the committed Android Kotlin tests.
/// Provenance (sha256 at transcription time): Bip39Test.kt 18e56933e8b0f2d4db1aba88f811f3ad4ef20a6a05ba8a3b847143fa76fc4241,
/// Bip32Test.kt 9ec8ffab6aff2d67c65bad8e335a4e84c3d051807baedeb6df93fb9535861c09,
/// EcdsaTest.kt 6b0b5e9a3e9243e1301f6b53a5f2e4ceb95269b0a896df305f671d9c8212f1e3.
/// Never edit by hand: regenerate from the Kotlin sources and re-record the hashes.
enum Vectors {
  struct Bip39Vector { let id: String; let entropyHex: String; let mnemonic: String; let seedHex: String }
  struct Chain { let path: String; let xprv: String; let xpub: String }
  struct OfficialVector { let id: String; let seedHex: String; let chains: [Chain] }
  struct PinnedSignature { let message: String; let signature: String; let normalized: Bool }
  struct Erc55Vector { let lower: String; let checksummed: String }

  static let bip39: [Bip39Vector] = [
    Bip39Vector(id: "V1", entropyHex: "00000000000000000000000000000000", mnemonic: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about", seedHex: "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04"),
    Bip39Vector(id: "V2", entropyHex: "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f", mnemonic: "legal winner thank year wave sausage worth useful legal winner thank yellow", seedHex: "2e8905819b8723fe2c1d161860e5ee1830318dbf49a83bd451cfb8440c28bd6fa457fe1296106559a3c80937a1c1069be3a3a5bd381ee6260e8d9739fce1f607"),
    Bip39Vector(id: "V3", entropyHex: "80808080808080808080808080808080", mnemonic: "letter advice cage absurd amount doctor acoustic avoid letter advice cage above", seedHex: "d71de856f81a8acc65e6fc851a38d4d7ec216fd0796d0a6827a3ad6ed5511a30fa280f12eb2e47ed2ac03b5c462a0358d18d69fe4f985ec81778c1b370b652a8"),
    Bip39Vector(id: "V4", entropyHex: "ffffffffffffffffffffffffffffffff", mnemonic: "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong", seedHex: "ac27495480225222079d7be181583751e86f571027b0497b5b5d11218e0a8a13332572917f0f8e5a589620c6f15b11c61dee327651a14c34e18231052e48c069"),
    Bip39Vector(id: "V5", entropyHex: "000000000000000000000000000000000000000000000000", mnemonic: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon agent", seedHex: "035895f2f481b1b0f01fcf8c289c794660b289981a78f8106447707fdd9666ca06da5a9a565181599b79f53b844d8a71dd9f439c52a3d7b3e8a79c906ac845fa"),
    Bip39Vector(id: "V6", entropyHex: "0000000000000000000000000000000000000000000000000000000000000000", mnemonic: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art", seedHex: "bda85446c68413707090a52022edd26a1c9462295029f2e60cd7c4f2bbd3097170af7a4d73245cafa9c3cca8d561a7c3de6f5d4a10be8ed2a5e608d68f92fcc8"),
    Bip39Vector(id: "V7", entropyHex: "0000000000000000000000000000000000000000", mnemonic: "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon address", seedHex: "fa08713f46bf5cb48728ceb70e3aae1bc53c5cb7b4e29c5610261d1cbb7be3bed4d805256fec515754d2be35974fc5da678168e9d9bb0cb70948026923b0def3"),
    Bip39Vector(id: "V8", entropyHex: "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff", mnemonic: "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo veteran", seedHex: "4aa0af4ca02ef1d9fa675cd02aa06d318425564e7fadd3d51b6165cc56d77398f28d8522073cd036c2a4a24a83e919211c84500d96cb120084e613ff5fcd96c1"),
  ]

  static let bip32: [OfficialVector] = [
    OfficialVector(id: "V1", seedHex: "000102030405060708090a0b0c0d0e0f", chains: [
      Chain(path: "m", xprv: "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi", xpub: "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"),
      Chain(path: "m/0'", xprv: "xprv9uHRZZhk6KAJC1avXpDAp4MDc3sQKNxDiPvvkX8Br5ngLNv1TxvUxt4cV1rGL5hj6KCesnDYUhd7oWgT11eZG7XnxHrnYeSvkzY7d2bhkJ7", xpub: "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8PX9rL2dZXvgGDnw"),
      Chain(path: "m/0'/1", xprv: "xprv9wTYmMFdV23N2TdNG573QoEsfRrWKQgWeibmLntzniatZvR9BmLnvSxqu53Kw1UmYPxLgboyZQaXwTCg8MSY3H2EU4pWcQDnRnrVA1xe8fs", xpub: "xpub6ASuArnXKPbfEwhqN6e3mwBcDTgzisQN1wXN9BJcM47sSikHjJf3UFHKkNAWbWMiGj7Wf5uMash7SyYq527Hqck2AxYysAA7xmALppuCkwQ"),
      Chain(path: "m/0'/1/2'", xprv: "xprv9z4pot5VBttmtdRTWfWQmoH1taj2axGVzFqSb8C9xaxKymcFzXBDptWmT7FwuEzG3ryjH4ktypQSAewRiNMjANTtpgP4mLTj34bhnZX7UiM", xpub: "xpub6D4BDPcP2GT577Vvch3R8wDkScZWzQzMMUm3PWbmWvVJrZwQY4VUNgqFJPMM3No2dFDFGTsxxpG5uJh7n7epu4trkrX7x7DogT5Uv6fcLW5"),
      Chain(path: "m/0'/1/2'/2", xprv: "xprvA2JDeKCSNNZky6uBCviVfJSKyQ1mDYahRjijr5idH2WwLsEd4Hsb2Tyh8RfQMuPh7f7RtyzTtdrbdqqsunu5Mm3wDvUAKRHSC34sJ7in334", xpub: "xpub6FHa3pjLCk84BayeJxFW2SP4XRrFd1JYnxeLeU8EqN3vDfZmbqBqaGJAyiLjTAwm6ZLRQUMv1ZACTj37sR62cfN7fe5JnJ7dh8zL4fiyLHV"),
      Chain(path: "m/0'/1/2'/2/1000000000", xprv: "xprvA41z7zogVVwxVSgdKUHDy1SKmdb533PjDz7J6N6mV6uS3ze1ai8FHa8kmHScGpWmj4WggLyQjgPie1rFSruoUihUZREPSL39UNdE3BBDu76", xpub: "xpub6H1LXWLaKsWFhvm6RVpEL9P4KfRZSW7abD2ttkWP3SSQvnyA8FSVqNTEcYFgJS2UaFcxupHiYkro49S8yGasTvXEYBVPamhGW6cFJodrTHy"),
    ]),
    OfficialVector(id: "V2", seedHex: "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542", chains: [
      Chain(path: "m", xprv: "xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", xpub: "xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB"),
      Chain(path: "m/0", xprv: "xprv9vHkqa6EV4sPZHYqZznhT2NPtPCjKuDKGY38FBWLvgaDx45zo9WQRUT3dKYnjwih2yJD9mkrocEZXo1ex8G81dwSM1fwqWpWkeS3v86pgKt", xpub: "xpub69H7F5d8KSRgmmdJg2KhpAK8SR3DjMwAdkxj3ZuxV27CprR9LgpeyGmXUbC6wb7ERfvrnKZjXoUmmDznezpbZb7ap6r1D3tgFxHmwMkQTPH"),
      Chain(path: "m/0/2147483647'", xprv: "xprv9wSp6B7kry3Vj9m1zSnLvN3xH8RdsPP1Mh7fAaR7aRLcQMKTR2vidYEeEg2mUCTAwCd6vnxVrcjfy2kRgVsFawNzmjuHc2YmYRmagcEPdU9", xpub: "xpub6ASAVgeehLbnwdqV6UKMHVzgqAG8Gr6riv3Fxxpj8ksbH9ebxaEyBLZ85ySDhKiLDBrQSARLq1uNRts8RuJiHjaDMBU4Zn9h8LZNnBC5y4a"),
      Chain(path: "m/0/2147483647'/1", xprv: "xprv9zFnWC6h2cLgpmSA46vutJzBcfJ8yaJGg8cX1e5StJh45BBciYTRXSd25UEPVuesF9yog62tGAQtHjXajPPdbRCHuWS6T8XA2ECKADdw4Ef", xpub: "xpub6DF8uhdarytz3FWdA8TvFSvvAh8dP3283MY7p2V4SeE2wyWmG5mg5EwVvmdMVCQcoNJxGoWaU9DCWh89LojfZ537wTfunKau47EL2dhHKon"),
      Chain(path: "m/0/2147483647'/1/2147483646'", xprv: "xprvA1RpRA33e1JQ7ifknakTFpgNXPmW2YvmhqLQYMmrj4xJXXWYpDPS3xz7iAxn8L39njGVyuoseXzU6rcxFLJ8HFsTjSyQbLYnMpCqE2VbFWc", xpub: "xpub6ERApfZwUNrhLCkDtcHTcxd75RbzS1ed54G1LkBUHQVHQKqhMkhgbmJbZRkrgZw4koxb5JaHWkY4ALHY2grBGRjaDMzQLcgJvLJuZZvRcEL"),
      Chain(path: "m/0/2147483647'/1/2147483646'/2", xprv: "xprvA2nrNbFZABcdryreWet9Ea4LvTJcGsqrMzxHx98MMrotbir7yrKCEXw7nadnHM8Dq38EGfSh6dqA9QWTyefMLEcBYJUuekgW4BYPJcr9E7j", xpub: "xpub6FnCn6nSzZAw5Tw7cgR9bi15UV96gLZhjDstkXXxvCLsUXBGXPdSnLFbdpq8p9HmGsApME5hQTZ3emM2rnY5agb9rXpVGyy3bdW6EEgAtqt"),
    ]),
    OfficialVector(id: "V3", seedHex: "4b381541583be4423346c643850da4b320e46a87ae3d2a4e6da11eba819cd4acba45d239319ac14f863b8d5ab5a0d0c64d2e8a1e7d1457df2e5a3c51c73235be", chains: [
      Chain(path: "m", xprv: "xprv9s21ZrQH143K25QhxbucbDDuQ4naNntJRi4KUfWT7xo4EKsHt2QJDu7KXp1A3u7Bi1j8ph3EGsZ9Xvz9dGuVrtHHs7pXeTzjuxBrCmmhgC6", xpub: "xpub661MyMwAqRbcEZVB4dScxMAdx6d4nFc9nvyvH3v4gJL378CSRZiYmhRoP7mBy6gSPSCYk6SzXPTf3ND1cZAceL7SfJ1Z3GC8vBgp2epUt13"),
      Chain(path: "m/0'", xprv: "xprv9uPDJpEQgRQfDcW7BkF7eTya6RPxXeJCqCJGHuCJ4GiRVLzkTXBAJMu2qaMWPrS7AANYqdq6vcBcBUdJCVVFceUvJFjaPdGZ2y9WACViL4L", xpub: "xpub68NZiKmJWnxxS6aaHmn81bvJeTESw724CRDs6HbuccFQN9Ku14VQrADWgqbhhTHBaohPX4CjNLf9fq9MYo6oDaPPLPxSb7gwQN3ih19Zm4Y"),
    ]),
    OfficialVector(id: "V4", seedHex: "3ddd5602285899a946114506157c7997e5444528f3003f6134712147db19b678", chains: [
      Chain(path: "m", xprv: "xprv9s21ZrQH143K48vGoLGRPxgo2JNkJ3J3fqkirQC2zVdk5Dgd5w14S7fRDyHH4dWNHUgkvsvNDCkvAwcSHNAQwhwgNMgZhLtQC63zxwhQmRv", xpub: "xpub661MyMwAqRbcGczjuMoRm6dXaLDEhW1u34gKenbeYqAix21mdUKJyuyu5F1rzYGVxyL6tmgBUAEPrEz92mBXjByMRiJdba9wpnN37RLLAXa"),
      Chain(path: "m/0'", xprv: "xprv9vB7xEWwNp9kh1wQRfCCQMnZUEG21LpbR9NPCNN1dwhiZkjjeGRnaALmPXCX7SgjFTiCTT6bXes17boXtjq3xLpcDjzEuGLQBM5ohqkao9G", xpub: "xpub69AUMk3qDBi3uW1sXgjCmVjJ2G6WQoYSnNHyzkmdCHEhSZ4tBok37xfFEqHd2AddP56Tqp4o56AePAgCjYdvpW2PU2jbUPFKsav5ut6Ch1m"),
      Chain(path: "m/0'/1'", xprv: "xprv9xJocDuwtYCMNAo3Zw76WENQeAS6WGXQ55RCy7tDJ8oALr4FWkuVoHJeHVAcAqiZLE7Je3vZJHxspZdFHfnBEjHqU5hG1Jaj32dVoS6XLT1", xpub: "xpub6BJA1jSqiukeaesWfxe6sNK9CCGaujFFSJLomWHprUL9DePQ4JDkM5d88n49sMGJxrhpjazuXYWdMf17C9T5XnxkopaeS7jGk1GyyVziaMt"),
    ]),
  ]

  static let pinnedSignatures: [PinnedSignature] = [
    PinnedSignature(message: "nexvault-test-vector", signature: "36eba619abb6f0c6aea1f504ca79613270ef55b72313036ce574dad1e87106ea72aa8de93f14ca6e5921b06ece96fe727fc6f4573d70090cc9556f25745f459501", normalized: false),
    PinnedSignature(message: "nexvault-test-vector-2", signature: "631e7a792cece9ea44f574fe6dba7e1ce1b62fa462d53767801980a37a9a79bd00783a080ad2c9d3881163b882c5fb2c9f890dc470623a10ab8c7c948f1bbe3800", normalized: true),
    PinnedSignature(message: "nexvault-test-vector-3", signature: "4a6723438513007fa3ed57c002bbe811ac58031fc8fcfe1179c43192fd6df4b811abce9d9a0b66140d746b1d7aea9ae143591f6a1a12d7ca887b922f6221fd5300", normalized: false),
    PinnedSignature(message: "nexvault-test-vector-4", signature: "0498077fe131fd453e4806e59173fa37dc8d2c807bfbe39d93d44be6f0b3410c4e3f3b0e5452bb16a6b431d1d27976c8bbeade1c098ac05eb46181042e43415c00", normalized: true),
    PinnedSignature(message: "nexvault-test-vector-5", signature: "37c016be7e875c7c24bb8f53ca2e05ed48a5aa4380a1ff7b83faf30a0b2d427f7cd029092cecdc3adb02e0095fa9f37c88ea2e6f268fab58b9bdfc7931cdbcbc01", normalized: false),
    PinnedSignature(message: "nexvault-test-vector-6", signature: "91db147e1c93355f36ce479b2eb7ceccbcd7087d43dac3afa10c6b4e8814e0a076106bbe859160f5847d0c889cf9faecef661bd857f27098658ad19d285f521600", normalized: true),
  ]

  static let erc55: [Erc55Vector] = [
    Erc55Vector(lower: "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed", checksummed: "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"),
    Erc55Vector(lower: "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359", checksummed: "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"),
    Erc55Vector(lower: "0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb", checksummed: "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB"),
    Erc55Vector(lower: "0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb", checksummed: "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"),
  ]

  static let canonicalMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
  static let canonicalPath = "m/44'/60'/0'/0/0"
  static let canonicalAddress = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"
  static let secondAddress = "0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0"
  static let probePrivateKey = "C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721"
  static let probeR = "432310e32cb80eb6503a26ce83cc165c783b870845fb8aad6d970889fcd7a6c8"
  static let probeS = "530128b6b81c548874a6305d93ed071ca6e05074d85863d4056ce89b02bfab69"
  static let canonicalPrivateKey = "1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727"
  static let canonicalSignature = "36eba619abb6f0c6aea1f504ca79613270ef55b72313036ce574dad1e87106ea72aa8de93f14ca6e5921b06ece96fe727fc6f4573d70090cc9556f25745f459501"
  static let digestMessage = "nexvault-test-vector"
  static let wordlistSha256 = "2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda"
  static let keccakEmpty = "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"
  static let keccakAbc = "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45"
}
