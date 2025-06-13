package com.fsvinative;

import android.os.Bundle; // <--- Add this import
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;

public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "fsviNative"; // This should match the name registered in AppRegistry.registerComponent() in index.js
  }

  /**
   * Returns the instance of the {@link ReactActivityDelegate}. Here we use a util class {@link
   * DefaultReactActivityDelegate} which allows you to easily enable Fabric and Concurrent React
   * (aka React 18) with two boolean flags.
   */
  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    return new DefaultReactActivityDelegate(
        this,
        getMainComponentName(),
        // If you opted-in for the New Architecture, we enable the Fabric Renderer.
        DefaultNewArchitectureEntryPoint.getFabricEnabled());
  }

  /**
   * Required for react-native-screens and some other navigation/UI libraries
   * to properly handle activity state restoration.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(null); // <--- Add this line, or super.onCreate(savedInstanceState) if you need to restore state via the Bundle
  }
}
