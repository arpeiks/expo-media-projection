import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoMediaProjectionViewProps } from './ExpoMediaProjection.types';

const NativeView: React.ComponentType<ExpoMediaProjectionViewProps> =
  requireNativeView('ExpoMediaProjection');

export default function ExpoMediaProjectionView(props: ExpoMediaProjectionViewProps) {
  return <NativeView {...props} />;
}
