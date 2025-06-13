# Video Input Monitor (React Native)

Video Input Monitor is a React Native application for Android that allows you to monitor camera feeds with a simple interface. The app includes the ability to switch between the front and back cameras and supports Android Picture‑in‑Picture (PIP) mode.

## Features

- **Camera Selection** – Easily switch between front and back cameras.
- **Picture‑in‑Picture** – Enter PIP mode to keep the camera view visible while using other apps (Android only).

## Getting Started

### Prerequisites

- **Node.js** (v18 or later)
- **npm**
- **Expo CLI** (`npm install -g expo-cli`)

### Installation

```sh
npm install
```

### Running on Android

```sh
npm run android
```

### Building the APK

The repository includes a GitHub Actions workflow that uses Expo Application Services (EAS) to create a release build. Set the `EAS_TOKEN` secret in your repository before triggering the workflow.

You can also build locally with:

```sh
npx eas build --platform android --profile production
```

## Project Structure

- `App.js` – Main application component.
- `index.js` – Entry point registered with Expo.
- `app.json` – Expo configuration file.
- You can add an `assets/` directory with your app icons if needed.

## License

This project is licensed under the MIT License.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
