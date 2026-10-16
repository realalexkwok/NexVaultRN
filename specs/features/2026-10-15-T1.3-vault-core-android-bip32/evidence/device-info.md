# T1.3 device evidence — Pixel 6a / Android 17 (2026-10-15)

Captured during the T1.3 H3 device run (the run whose XML is `device-TEST-Pixel6a-2026-10-15.xml`).
Kept inside the feature directory because the Gradle build dir is gitignored and gets overwritten
(2026-10-16 code-review round, NIT-3/NIT-4).

```bash
$ adb devices -l
10.42.0.139:41771      device product:bluejay model:Pixel_6a device:bluejay transport_id:6
adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp device product:bluejay model:Pixel_6a device:bluejay transport_id:5
# two transports for one phone → the run pinned ANDROID_SERIAL=10.42.0.139:41771 (T1.2 lesson)

$ adb -s 10.42.0.139:41771 shell getprop ro.serialno          → 26111jEGR13989
$ adb -s 10.42.0.139:41771 shell getprop ro.product.model     → Pixel 6a
$ adb -s 10.42.0.139:41771 shell getprop ro.build.version.release → 17
$ adb -s 10.42.0.139:41771 shell getprop ro.build.version.sdk → 37
$ adb -s 10.42.0.139:41771 shell getprop ro.build.fingerprint →
    google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys

$ ANDROID_SERIAL=10.42.0.139:41771 ./gradlew :vault-core:connectedDebugAndroidTest
Starting 4 tests on Pixel 6a - 17
Finished 4 tests on Pixel 6a - 17
BUILD SUCCESSFUL in 4m 22s        (gradle_exit=0)
```

Result XML: `tests="4" skipped="0" failures="0" errors="0"`, `timestamp="2026-10-15T18:51:49"`.
Cases: `rawWordlistHasExactly2048Words`, `moduleDefinitionExposesImportMnemonicAndDeriveAddress`,
`canonicalAddressOnDevice`, `invalidPathExposesTypedCode`.

Provenance (relabelled 2026-10-16, MINOR-R2-3):

- The `getprop` values were **captured live** with `adb -s 10.42.0.139:41771 shell getprop …` in the
  T1.3 implementation session on 2026-10-15, immediately before the run, and were quoted verbatim in
  that session's command output (the same values are transcribed in `validation.md` Task 6).
- **This file itself is a 2026-10-16 transcription** of that session record — `adb` was *not*
  attached when it was written, so it is a reconstruction from the 2026-10-15 evidence, not a live
  capture at write time.
- `device-TEST-Pixel6a-2026-10-15.xml` and `device-test-result-2026-10-15.pb` are the run's own
  artifacts (copied from `modules/vault-core/android/build/outputs/androidTest-results/connected/debug/`
  on 2026-10-16, before that gitignored directory was overwritten).
- `device-run-2026-10-15.log` is the **terminal `tee` capture** of the pinned Gradle invocation; its
  mtime is the 2026-10-16 copy time. It is not a UTP artifact, so its hash does not match the
  preserved `utp.0.log` / `test-results.log`.
- Next device session: capture `getprop` **live** into this folder again (NIT-3/NIT-4 follow-up).
