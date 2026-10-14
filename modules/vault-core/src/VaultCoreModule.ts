import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {}

export default requireNativeModule<VaultCoreModule>('VaultCore');
