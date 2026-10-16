import Darwin

/// Wipes a mutable byte buffer with `memset_s`, which the optimizer may not elide — the iOS side
/// of the R1 zeroization discipline. Swift `Data`/`String` copies cannot be wiped (CoW/heap);
/// those residuals are documented as **[C]** caveats in validation.md, never [P].
func wipe(_ bytes: inout [UInt8]) {
  guard !bytes.isEmpty else { return }
  bytes.withUnsafeMutableBytes { buffer in
    if let base = buffer.baseAddress {
      memset_s(base, buffer.count, 0, buffer.count)
    }
  }
}
