import { registerWebModule, NativeModule } from 'expo';

class VaultCoreModule extends NativeModule<{}> {}

export default registerWebModule(VaultCoreModule, 'VaultCoreModule');
