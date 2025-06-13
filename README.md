# Video Input Monitor for Android

Video Input Monitor for Android is a React Native application that allows users to monitor device camera feeds with a clean interface.

## Features

- **Live Camera View**: Displays live feed from the selected device camera.
- **Camera Switching**: Easily switch between front and back cameras.
- **Fullscreen Mode**: Hides the system status bar for a more immersive view.

## Prerequisites

- Node.js (v18 or later recommended)
- npm (included with Node.js) or Yarn
- React Native development environment: Follow the official guide for "Setting up the development environment" on the React Native website, selecting "React Native CLI Quickstart" for your OS. This includes installing an Android SDK and JDK.
- Android Emulator or a connected Android device.

## Installation & Running

1.  **Clone the repository:**
    ```sh
    git clone <your-repo-url>
    cd <your-repo-name>
    ```

2.  **Install dependencies:**
    ```sh
    npm install
    # OR
    # yarn install
    ```

3.  **Run the application:**
    ```sh
    npx react-native run-android
    # OR (if scripts are preferred)
    # npm run android
    # yarn android
    ```

## Building for Release (Local)

To build a release version of the app locally:

1.  **Generate a signing keystore:**
    If you don't have one, generate it using `keytool`:
    ```bash
    keytool -genkeypair -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
    ```
    Place the `my-release-key.keystore` file in the `android/app/` directory. **Important:** Add `my-release-key.keystore` to your `.gitignore` file if you haven't already!

2.  **Configure keystore credentials:**
    Create a file named `android/gradle.properties` (if it doesn't exist) and add the following lines (replace with your actual credentials). **Important:** Add `gradle.properties` to your `.gitignore` file!
    ```properties
    MYAPP_RELEASE_STORE_FILE=my-release-key.keystore
    MYAPP_RELEASE_KEY_ALIAS=my-key-alias
    MYAPP_RELEASE_STORE_PASSWORD=your_store_password
    MYAPP_RELEASE_KEY_PASSWORD=your_key_password
    ```

3.  **Build the App Bundle (AAB) or APK:**
    Navigate to the `android` directory:
    ```sh
    cd android
    ```
    To build an AAB (for Google Play Store):
    ```sh
    ./gradlew bundleRelease
    ```
    The AAB will be located at `android/app/build/outputs/bundle/release/app-release.aab`.

    To build an APK (for direct installs):
    ```sh
    ./gradlew assembleRelease
    ```
    The APK will be located at `android/app/build/outputs/apk/release/app-release.apk`.
    Navigate back with `cd ..`

## CI/CD with GitHub Actions

This project uses GitHub Actions for Continuous Integration and Continuous Deployment:

-   **`android-ci.yml` (Build & Test):**
    -   Triggered on pushes and pull requests to `main` and `develop` branches.
    -   Sets up the environment, installs dependencies.
    -   Builds the Android App Bundle (AAB) to ensure the app builds correctly.
    -   Uploads the AAB as a build artifact for inspection.

-   **`android-release.yml` (Release to Play Store & GitHub):**
    -   Triggered on pushes to tags matching `v*.*.*` (e.g., `v1.0.0`, `v1.0.1-beta`).
    -   Can also be triggered manually via the GitHub Actions UI (`workflow_dispatch`).
    -   Builds a **signed** AAB and APK using secrets.
    -   Uploads the AAB to the Google Play Store (to the `internal` track by default).
    -   Creates a GitHub Release, attaching the generated AAB and APK files.

### Required GitHub Secrets for Release Workflow:
For the `android-release.yml` workflow to publish to the Play Store and create signed artifacts, the following secrets must be configured in your GitHub repository settings (under "Secrets and variables" > "Actions"):
-   `ANDROID_RELEASE_KEYSTORE_BASE64`: The base64 encoded content of your release keystore file.
-   `ANDROID_RELEASE_KEY_ALIAS`: The alias for your release key.
-   `ANDROID_RELEASE_STORE_PASSWORD`: Your keystore password.
-   `ANDROID_RELEASE_KEY_PASSWORD`: Your release key's password.
-   `GOOGLE_PLAY_JSON_KEY_BASE64`: The base64 encoded JSON service account key from Google Play Console with permissions to upload app bundles.

## License

This project is licensed under the MIT License.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
