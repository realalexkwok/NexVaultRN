import Foundation

/// Self-written RIPEMD-160 (CommonCrypto ships none). Used for BIP-32 HASH160 fingerprints and
/// the test-side extended-key serializer — public data only; gated by the official BIP-32 vectors.
enum Ripemd160 {
  private static let leftIndex: [Int] = [
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
    3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
    1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
    4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13,
  ]
  private static let rightIndex: [Int] = [
    5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
    6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
    15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
    8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
    12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11,
  ]
  private static let leftShift: [Int] = [
    11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
    7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
    11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
    11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
    9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6,
  ]
  private static let rightShift: [Int] = [
    8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
    9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
    9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
    15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
    8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11,
  ]
  private static let leftConstant: [UInt32] = [0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC, 0xA953FD4E]
  private static let rightConstant: [UInt32] = [0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x7A6D76E9, 0x00000000]

  static func hash(_ message: [UInt8]) -> [UInt8] {
    var h0: UInt32 = 0x67452301
    var h1: UInt32 = 0xEFCDAB89
    var h2: UInt32 = 0x98BADCFE
    var h3: UInt32 = 0x10325476
    var h4: UInt32 = 0xC3D2E1F0

    var padded = message
    let bitLength = UInt64(message.count) * 8
    padded.append(0x80)
    while padded.count % 64 != 56 {
      padded.append(0)
    }
    for byteIndex in 0..<8 {
      padded.append(UInt8(truncatingIfNeeded: bitLength >> UInt64(byteIndex * 8)))
    }

    var offset = 0
    while offset < padded.count {
      var words = [UInt32](repeating: 0, count: 16)
      for index in 0..<16 {
        let base = offset + index * 4
        words[index] = UInt32(padded[base])
          | (UInt32(padded[base + 1]) << 8)
          | (UInt32(padded[base + 2]) << 16)
          | (UInt32(padded[base + 3]) << 24)
      }

      var al = h0, bl = h1, cl = h2, dl = h3, el = h4
      var ar = h0, br = h1, cr = h2, dr = h3, er = h4
      for round in 0..<80 {
        let group = round / 16
        var t = rotateLeft(al &+ f(group, bl, cl, dl) &+ words[leftIndex[round]] &+ leftConstant[group], leftShift[round]) &+ el
        al = el
        el = dl
        dl = rotateLeft(cl, 10)
        cl = bl
        bl = t
        t = rotateLeft(ar &+ f(4 - group, br, cr, dr) &+ words[rightIndex[round]] &+ rightConstant[group], rightShift[round]) &+ er
        ar = er
        er = dr
        dr = rotateLeft(cr, 10)
        cr = br
        br = t
      }
      let combined = h1 &+ cl &+ dr
      h1 = h2 &+ dl &+ er
      h2 = h3 &+ el &+ ar
      h3 = h4 &+ al &+ br
      h4 = h0 &+ bl &+ cr
      h0 = combined
      offset += 64
    }

    var output = [UInt8]()
    output.reserveCapacity(20)
    for word in [h0, h1, h2, h3, h4] {
      for byteIndex in 0..<4 {
        output.append(UInt8(truncatingIfNeeded: word >> UInt64(byteIndex * 8)))
      }
    }
    return output
  }

  private static func f(_ group: Int, _ x: UInt32, _ y: UInt32, _ z: UInt32) -> UInt32 {
    switch group {
    case 0: return x ^ y ^ z
    case 1: return (x & y) | (~x & z)
    case 2: return (x | ~y) ^ z
    case 3: return (x & z) | (y & ~z)
    default: return x ^ (y | ~z)
    }
  }

  private static func rotateLeft(_ value: UInt32, _ amount: Int) -> UInt32 {
    amount == 0 ? value : (value << UInt32(amount)) | (value >> UInt32(32 - amount))
  }
}
