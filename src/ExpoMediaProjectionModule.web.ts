import { registerWebModule, NativeModule } from 'expo';

import { ExpoMediaProjectionModuleEvents } from './ExpoMediaProjection.types';

class ExpoMediaProjectionModule extends NativeModule<ExpoMediaProjectionModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoMediaProjectionModule);
