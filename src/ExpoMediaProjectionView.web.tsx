import * as React from 'react';

import { ExpoMediaProjectionViewProps } from './ExpoMediaProjection.types';

export default function ExpoMediaProjectionView(props: ExpoMediaProjectionViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
