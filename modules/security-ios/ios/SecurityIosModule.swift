import ExpoModulesCore

/**
 * T3.1 iOS module (author-only on this Linux host; compiled and tested on the Mac per
 * MAC-HANDOFF.md).
 *
 * The minimal JS face is src/index.ts (ensureKek / deleteKek via expo-secure-store, spec
 * section 2.2). wrap / unwrap stay native-internal inside WrapKeyService and land on the JS
 * surface in T3.2 together with the vault-core coupling.
 */
public class SecurityIosModule: Module {
  public func definition() -> ModuleDefinition {
    Name("SecurityIos")
  }
}
