import React from "react";
import * as MediaLibrary from "expo-media-library";
import ExpoMediaProjection from "expo-media-projection";
import { Button, SafeAreaView, ScrollView, Text, View } from "react-native";

export default function App() {
  const path = "MyPictures";
  const [mediaPermission, requestMediaPermission] = MediaLibrary.usePermissions({ granularPermissions: ["photo"] });

  const stop = async () => {
    await ExpoMediaProjection.stop();
  };

  const takeScreenshot = async () => {
    const res = await ExpoMediaProjection.takeScreenshot();
    console.log(res);
  };

  const start = async () => {
    const overlay = await ExpoMediaProjection.askForOverlayPermission();
    if (!overlay) return;

    const mediaProjection = await ExpoMediaProjection.askMediaProjectionPermission({
      path,
      pathType: "FOLDER",
    });

    if (!mediaProjection) return;
    await ExpoMediaProjection.showScreenshotButton();
  };

  const getAlbum = async (after?: MediaLibrary.Asset) => {
    const status = mediaPermission?.status;
    if (status !== "granted") await requestMediaPermission();

    const album = await MediaLibrary.getAlbumAsync(path);
    console.log("Album: ", JSON.stringify(album, null, 2));

    if (!album?.assetCount) return [null, new Error("Album not available")];

    const mediaLibrary = await MediaLibrary.getAssetsAsync({
      album,
      after,
      first: 1,
      mediaType: "photo",
    });

    console.log("MediaLibrary: ", JSON.stringify(mediaLibrary, null, 2));
  };

  React.useEffect(() => {
    const subscription = MediaLibrary.addListener(() => {
      console.log("Subscription triggered");
      getAlbum();
    });

    return () => subscription.remove();
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Module API Example</Text>

        <Group name="Start">
          <Button title="Start" onPress={start} />
        </Group>

        <Group name="Take Screenshot">
          <Button title="Take Screenshot" onPress={takeScreenshot} />
        </Group>

        <Group name="Stop">
          <Button title="Stop" onPress={stop} />
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
  view: {
    flex: 1,
    height: 200,
  },
};
