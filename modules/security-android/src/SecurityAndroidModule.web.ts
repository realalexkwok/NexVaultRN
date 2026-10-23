import { registerWebModule, NativeModule } from 'expo';

// Web is a read-only observer (AGENTS.md R7): the web bundle carries no keystore or wrap-key path.
class SecurityAndroidModule extends NativeModule<{}> {}

export default registerWebModule(SecurityAndroidModule, 'SecurityAndroidModule');
