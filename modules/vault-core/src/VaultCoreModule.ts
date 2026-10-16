import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
  deriveAddress(handle: string, path: string): Promise<string>;
  signDigest(handle: string, path: string, digest: Uint8Array): Promise<Uint8Array>;
  destroy(handle: string): Promise<void>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
