import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  ActivityIndicator,
  Linking,
  Platform,
  TouchableOpacity,
} from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Colors } from 'react-native/Libraries/NewAppScreen';

// Vision Camera imports
import { Camera, useCameraDevice, useCameraPermission } from 'react-native-vision-camera';

// HomeScreen component modified for Camera
const HomeScreen = () => {
  const isDarkMode = useColorScheme() === 'dark';
  const { hasPermission, requestPermission } = useCameraPermission();
  const [permissionStatus, setPermissionStatus] = useState('loading');
  const [cameraPosition, setCameraPosition] = useState('back');
  const device = useCameraDevice(cameraPosition);
  const [isStatusBarHidden, setIsStatusBarHidden] = useState(false); // New state for status bar

  useEffect(() => {
    const checkPermission = async () => {
      if (hasPermission) {
        setPermissionStatus('granted');
        return;
      }
      const granted = await requestPermission();
      setPermissionStatus(granted ? 'granted' : 'denied');
    };
    checkPermission();
  }, [hasPermission, requestPermission]);

  const toggleCameraPosition = () => {
    setCameraPosition((prevPosition) => (prevPosition === 'back' ? 'front' : 'back'));
  };

  const toggleStatusBar = () => {
    setIsStatusBarHidden(!isStatusBarHidden);
  };

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
    flex: 1,
  };

  const renderCameraView = () => {
    if (permissionStatus === 'loading' || (permissionStatus === 'granted' && device == null && Platform.OS !== 'android')) {
      return (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={isDarkMode ? Colors.white : Colors.black} />
          <Text style={styles.statusText(isDarkMode)}>Loading camera...</Text>
        </View>
      );
    }

    if (permissionStatus === 'denied') {
      return (
        <View style={styles.centered}>
          <Text style={styles.statusText(isDarkMode)}>Camera permission denied.</Text>
          <Text style={styles.linkText} onPress={() => Linking.openSettings()}>
            Open Settings to Grant Permission
          </Text>
        </View>
      );
    }

    if (device == null) {
        return (
            <View style={styles.centered}>
                <Text style={styles.statusText(isDarkMode)}>
                  {Platform.OS === 'android' ? 'No camera device found.' : 'Camera not available.'}
                </Text>
            </View>
        );
    }

    return (
      <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
      />
    );
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
        hidden={isStatusBarHidden} // Control visibility with state
        translucent={true} // Often good for fullscreen apps
      />
      {renderCameraView()}
      {permissionStatus === 'granted' && device != null && (
        <View style={styles.controlsContainer}>
          <TouchableOpacity style={styles.controlButton} onPress={toggleCameraPosition}>
            <Text style={styles.controlButtonText}>Switch</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.controlButton} onPress={toggleStatusBar}>
            <Text style={styles.controlButtonText}>Full</Text>
          </TouchableOpacity>
        </View>
      )}
    </SafeAreaView>
  );
};

const Stack = createNativeStackNavigator();

const App = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen
          name="Home"
          component={HomeScreen}
          options={{
            title: 'fsvi Camera',
            headerShown: false
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  statusText: (isDarkMode) => ({
    fontSize: 18,
    textAlign: 'center',
    marginBottom: 10,
    color: isDarkMode ? Colors.light : Colors.dark,
  }),
  linkText: {
    fontSize: 16,
    color: Platform.OS === 'ios' ? Colors.blue : Colors.primary,
    textDecorationLine: 'underline',
  },
  controlsContainer: {
    position: 'absolute',
    top: Platform.OS === 'ios' ? 50 : 20,
    right: 20,
    zIndex: 10,
    flexDirection: 'row',
  },
  controlButton: {
    backgroundColor: 'rgba(0,0,0,0.5)',
    paddingVertical: 10,
    paddingHorizontal: 15,
    borderRadius: 8,
    marginLeft: 10, // Space between buttons
  },
  controlButtonText: {
    color: 'white',
    fontSize: 16,
    textAlign: 'center',
  },
  sectionTitle: (isDarkMode) => ({
    fontSize: 24,
    fontWeight: '600',
    textAlign: 'center',
    color: isDarkMode ? Colors.white : Colors.black,
  }),
  sectionDescription: (isDarkMode) => ({
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
    textAlign: 'center',
    color: isDarkMode ? Colors.light : Colors.dark,
  }),
});

export default App;
