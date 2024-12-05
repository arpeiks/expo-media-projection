import { requireNativeModule } from "expo";

type MediaProjectionOptions = {
  path: string;
  pathType: "PATH" | "FOLDER";
};

declare class ExpoMediaProjectionModule {
  stop(): Promise<boolean>;
  takeScreenshot(): Promise<boolean>;
  showScreenshotButton(): Promise<boolean>;
  askForOverlayPermission(): Promise<boolean>;
  askMediaProjectionPermission(options: MediaProjectionOptions): Promise<boolean>;
}

export default requireNativeModule<ExpoMediaProjectionModule>("ExpoMediaProjection");
