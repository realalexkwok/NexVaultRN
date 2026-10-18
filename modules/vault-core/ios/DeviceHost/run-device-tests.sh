#!/bin/bash
# T1.5-r2 device gate: run the crypto-core XCTest bundle on the iPad mini 4 (iOS 15.8.8).
#
# Xcode 26.5 refuses to test an iOS 15.x device ("Logic Testing Unavailable") no matter how the
# bundle is hosted (CocoaPods pod spec, app-host scaffolding, or the plain DeviceHost project used
# here). An older Xcode (15.x / 16.x) supports on-device testing for iOS 15.
#
# Usage:  ./run-device-tests.sh /Applications/Xcode_16.4.app     (omit to use the active Xcode)
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
PROJECT="$HERE/VaultCoreDeviceHost.xcodeproj"
DEVICE="dd3c667bc2c60856086f7cbfcbd23fcd5eebd252"   # 郭浩然的iPad, iPad mini 4, iOS 15.8.8
TEAM="F7Y3R4ZWAQ"

if [ "$#" -ge 1 ]; then
  export DEVELOPER_DIR="$1/Contents/Developer"
fi
echo "using: $(xcodebuild -version | tail -1)  ($(xcode-select -p))"

xcodebuild test \
  -project "$PROJECT" \
  -scheme VaultCoreCoreTests \
  -destination "platform=iOS,id=$DEVICE" \
  DEVELOPMENT_TEAM="$TEAM" \
  -allowProvisioningUpdates \
  -resultBundlePath /tmp/device-core-tests.xcresult
