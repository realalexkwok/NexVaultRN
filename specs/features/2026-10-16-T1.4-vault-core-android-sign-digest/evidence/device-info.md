# T1.4 device record

Captured 2026-10-16 during the T1.4 implementation session with
`adb -s adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp shell getprop <key>`
(transcribed from the live adb session; single transport, `ANDROID_SERIAL` pinned for the whole
run — the T1.2 duplicate-transport lesson).

| Property | Value |
| --- | --- |
| ro.product.model | Pixel 6a |
| ro.product.device | bluejay |
| ro.build.version.release | 17 |
| ro.build.version.sdk | 37 |
| ro.build.fingerprint | google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys |

adb transport: `adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp` (wireless adb, one entry in
`adb devices`).
