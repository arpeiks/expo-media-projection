import { requireNativeModule } from "expo";

declare class ExpoMediaProjectionModule {
  stop(): Promise<boolean>;
  takeScreenshot(): Promise<boolean>;
  showScreenshotButton(): Promise<boolean>;
  askForOverlayPermission(): Promise<boolean>;
  askMediaProjectionPermission(): Promise<boolean>;
}

export default requireNativeModule<ExpoMediaProjectionModule>("ExpoMediaProjection");
