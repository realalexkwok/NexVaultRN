import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
