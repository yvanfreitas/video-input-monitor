import React, { useState, useEffect } from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { Camera } from 'expo-camera';
import PipHandler from 'react-native-pip-android';

export default function App() {
  const [hasPermission, setHasPermission] = useState(null);
  const [type, setType] = useState(Camera.Constants.Type.front);

  useEffect(() => {
    (async () => {
      const { status } = await Camera.requestCameraPermissionsAsync();
      setHasPermission(status === 'granted');
    })();
  }, []);

  if (hasPermission === null) {
    return <View />;
  }
  if (hasPermission === false) {
    return <Text>No access to camera</Text>;
  }

  const toggleCamera = () => {
    setType(
      type === Camera.Constants.Type.front
        ? Camera.Constants.Type.back
        : Camera.Constants.Type.front
    );
  };

  return (
    <View style={{ flex: 1 }}>
      <Camera style={StyleSheet.absoluteFill} type={type} />
      <View style={styles.controls}>
        <TouchableOpacity onPress={toggleCamera} style={styles.button}>
          <Text style={styles.text}>Switch Camera</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => PipHandler.enterPipMode()} style={styles.button}>
          <Text style={styles.text}>PIP</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  controls: {
    position: 'absolute',
    bottom: 20,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  button: {
    backgroundColor: 'rgba(0,0,0,0.5)',
    padding: 10,
    borderRadius: 4,
  },
  text: {
    color: '#fff',
  },
});
