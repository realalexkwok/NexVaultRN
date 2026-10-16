import Foundation

/// Self-written Keccak-f[1600] sponge producing **pre-NIST Keccak-256** (T1.5 GATE-KECCAK —
/// CommonCrypto ships no Keccak). Padding is the original Keccak 0x01..0x80 scheme, never the
/// NIST SHA3 0x06 domain byte. Only public data flows through here (address bytes, EIP-55 hex).
enum Keccak {
  private static let roundConstants: [UInt64] = [
    0x0000000000000001, 0x0000000000008082, 0x800000000000808A, 0x8000000080008000,
    0x000000000000808B, 0x0000000080000001, 0x8000000080008081, 0x8000000000008009,
    0x000000000000008A, 0x0000000000000088, 0x0000000080008009, 0x000000008000000A,
    0x000000008000808B, 0x800000000000008B, 0x8000000000008089, 0x8000000000008003,
    0x8000000000008002, 0x8000000000000080, 0x000000000000800A, 0x800000008000000A,
    0x8000000080008081, 0x8000000000008080, 0x0000000080000001, 0x8000000080008008,
  ]
  private static let rotationConstants: [Int] = [
    1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 2, 14, 27, 41, 56, 8, 25, 43, 62, 18, 39, 61, 20, 44,
  ]
  private static let permutationIndices: [Int] = [
    10, 7, 11, 17, 18, 3, 5, 16, 8, 21, 24, 4, 15, 23, 19, 13, 12, 2, 20, 14, 22, 9, 6, 1,
  ]
  /// Keccak-256 rate: 1600 - 2*256 = 1088 bits = 136 bytes.
  private static let rate = 136

  static func keccak256(_ input: [UInt8]) -> [UInt8] {
    var state = [UInt64](repeating: 0, count: 25)
    var block = [UInt8](repeating: 0, count: rate)
    var offset = 0
    for byte in input {
      block[offset] = byte
      offset += 1
      if offset == rate {
        absorb(&state, block)
        offset = 0
      }
    }
    for index in offset..<rate {
      block[index] = 0
    }
    block[offset] = 0x01
    block[rate - 1] |= 0x80
    absorb(&state, block)

    var output = [UInt8](repeating: 0, count: 32)
    for index in 0..<4 {
      let lane = state[index]
      for byteIndex in 0..<8 {
        output[index * 8 + byteIndex] = UInt8(truncatingIfNeeded: lane >> UInt64(byteIndex * 8))
      }
    }
    return output
  }

  private static func absorb(_ state: inout [UInt64], _ block: [UInt8]) {
    for laneIndex in 0..<(rate / 8) {
      var lane: UInt64 = 0
      for byteIndex in 0..<8 {
        lane |= UInt64(block[laneIndex * 8 + byteIndex]) << UInt64(byteIndex * 8)
      }
      state[laneIndex] ^= lane
    }
    permute(&state)
  }

  private static func rotateLeft(_ value: UInt64, _ amount: Int) -> UInt64 {
    amount == 0 ? value : (value << UInt64(amount)) | (value >> UInt64(64 - amount))
  }

  private static func permute(_ a: inout [UInt64]) {
    var column = [UInt64](repeating: 0, count: 5)
    for round in 0..<24 {
      // theta
      for x in 0..<5 {
        column[x] = a[x] ^ a[x + 5] ^ a[x + 10] ^ a[x + 15] ^ a[x + 20]
      }
      for x in 0..<5 {
        let t = column[(x + 4) % 5] ^ rotateLeft(column[(x + 1) % 5], 1)
        for y in stride(from: 0, to: 25, by: 5) {
          a[y + x] ^= t
        }
      }
      // rho + pi
      var carry = a[1]
      for i in 0..<24 {
        let j = permutationIndices[i]
        let tmp = a[j]
        a[j] = rotateLeft(carry, rotationConstants[i])
        carry = tmp
      }
      // chi
      for y in stride(from: 0, to: 25, by: 5) {
        for x in 0..<5 {
          column[x] = a[y + x]
        }
        for x in 0..<5 {
          a[y + x] = column[x] ^ ((~column[(x + 1) % 5]) & column[(x + 2) % 5])
        }
      }
      // iota
      a[0] ^= roundConstants[round]
    }
  }
}
