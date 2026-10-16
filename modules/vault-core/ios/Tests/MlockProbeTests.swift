import Darwin
import XCTest
@testable import VaultCore

/// MAN-013 carrier (cross_verification C6): the "one-line" `mlock`/`munlock` probe, run as a test so
/// the raw result and errno land in the xcresult instead of a terminal transcript that nobody keeps.
///
/// MAN-013's acceptance **accepts either outcome** (success or EPERM/ENOMEM) — the probe exists only to
/// record what the iOS sandbox actually does, for the `[C]` labelling review. This test therefore
/// asserts that the probe executed and captured a definite result; it never asserts that locking
/// succeeded, and it has no functional dependency.
///
/// Authority: run it on the physical iPhone (B.4-5). A simulator run is **not** admissible for the
/// MAN-013 row — the macOS host sandbox is not the iOS sandbox — so a simulator result is recorded
/// only as "carrier works", never as the manual-walk conclusion.
final class MlockProbeTests: XCTestCase {
  func testMlockProbeRecordsResultAndErrno() throws {
    let pageSize = Int(getpagesize())
    XCTAssertGreaterThan(pageSize, 0)

    var allocated: UnsafeMutableRawPointer?
    let allocation = posix_memalign(&allocated, pageSize, pageSize)
    XCTAssertEqual(allocation, 0, "posix_memalign failed with errno " + String(allocation))
    let page = try XCTUnwrap(allocated)
    defer { free(page) }
    memset(page, 0, pageSize)

    errno = 0
    let lockResult = mlock(page, pageSize)
    let lockErrno = errno

    var unlockResult: Int32 = -1
    var unlockErrno: Int32 = 0
    if lockResult == 0 {
      errno = 0
      unlockResult = munlock(page, pageSize)
      unlockErrno = errno
    }

    let aligned = Int(bitPattern: page) % pageSize == 0
    let report = [
      "MAN-013 mlock probe",
      "page size: " + String(pageSize) + " B; page-aligned: " + String(aligned),
      "mlock   -> " + String(lockResult) + " (errno " + String(lockErrno) + ": " + String(cString: strerror(lockErrno)) + ")",
      "munlock -> " + (lockResult == 0
        ? String(unlockResult) + " (errno " + String(unlockErrno) + ": " + String(cString: strerror(unlockErrno)) + ")"
        : "not attempted (mlock did not succeed)"),
    ].joined(separator: "\n")
    let attachment = XCTAttachment(string: report)
    attachment.name = "MAN-013 mlock probe"
    attachment.lifetime = .keepAlways
    add(attachment)

    XCTAssertTrue(aligned, "the probe page must be page-aligned")
    XCTAssertTrue(
      lockResult == 0 || (lockResult == -1 && lockErrno != 0),
      "mlock must either lock the page or fail with a captured errno, got " + String(lockResult)
    )
    if lockResult == 0 {
      XCTAssertEqual(unlockResult, 0, "munlock must succeed for a page this process locked")
    }
  }
}
