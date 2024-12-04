import ExpoMediaProjection from "expo-media-projection";
import { Button, SafeAreaView, ScrollView, Text, View } from "react-native";

export default function App() {
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

    const mediaProjection = await ExpoMediaProjection.askMediaProjectionPermission();
    if (!mediaProjection) return;

    await ExpoMediaProjection.showScreenshotButton();
  };

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

        {/* <Group name="Events">
          <Text>{onChangePayload?.value}</Text>
        </Group> */}

        {/* <Group name="Views">
          <ExpoMediaProjectionView
            url="https://www.example.com"
            onLoad={({ nativeEvent: { url } }) => console.log(`Loaded: ${url}`)}
            style={styles.view}
          />
        </Group> */}
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
