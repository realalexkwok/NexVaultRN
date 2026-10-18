import UIKit

/// Minimal test host (T1.5-r2): no Expo, so it can target iOS 15.1 and run the crypto-core XCTest
/// bundle on an iOS 15.8 device. It intentionally does nothing at launch.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
  var window: UIWindow?

  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    let window = UIWindow(frame: UIScreen.main.bounds)
    window.rootViewController = UIViewController()
    window.makeKeyAndVisible()
    self.window = window
    return true
  }
}
