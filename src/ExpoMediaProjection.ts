import { requireNativeModule } from "expo";

declare class ExpoMediaProjection {
  stop(): Promise<boolean>;
  start(): Promise<boolean>;
  takeScreenshot(): Promise<boolean>;
  showScreenshotButton(): Promise<boolean>;
  askForOverlayPermission(): Promise<boolean>;
  askMediaProjectionPermission(): Promise<boolean>;
}

export default requireNativeModule<ExpoMediaProjection>("ExpoMediaProjection");
