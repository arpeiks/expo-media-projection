// Reexport the native module. On web, it will be resolved to ExpoMediaProjectionModule.web.ts
// and on native platforms to ExpoMediaProjectionModule.ts
export { default } from './ExpoMediaProjectionModule';
export { default as ExpoMediaProjectionView } from './ExpoMediaProjectionView';
export * from  './ExpoMediaProjection.types';
