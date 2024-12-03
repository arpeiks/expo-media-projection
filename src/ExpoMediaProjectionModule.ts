import { NativeModule, requireNativeModule } from 'expo';

import { ExpoMediaProjectionModuleEvents } from './ExpoMediaProjection.types';

declare class ExpoMediaProjectionModule extends NativeModule<ExpoMediaProjectionModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoMediaProjectionModule>('ExpoMediaProjection');
