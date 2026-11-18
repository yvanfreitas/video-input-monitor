# Video Input Monitor

Video Input Monitor is a cross-platform Electron application that allows users to monitor webcam feeds with a clean and intuitive interface. The application includes fullscreen and Picture-in-Picture (PIP) modes, making it ideal for multi-tasking while using webcam feeds.

## Features

- **Webcam Selection**: Easily switch between multiple webcam devices connected to your system.
- **Fullscreen Mode**: View the video feed in fullscreen mode for an immersive experience.
- **Picture-in-Picture (PIP)**: Keep the video feed visible in a floating window while working on other tasks.
- **Automatic Controls Hide**: The controls automatically hide after 5 seconds of inactivity for an unobstructed view.

## Getting Started

### Prerequisites

- **Node.js** 22.x (the version in `engines` of `package.json`)
- **npm** (included with Node.js)

### Installation

1. Clone the repository:

   ```sh
   git clone https://github.com/yvanfreitas/video-input-monitor.git
   cd video-input-monitor
   ```

2. Install the dependencies:

   ```sh
   npm install
   ```

### Running the Application

To start the application in development mode, use:

```sh
npm start
```

### Building for Distribution

The app is packaged with [Electron Forge](https://www.electronforge.io/) (`forge.config.js`). To build the packages for the platform you are on:

```sh
npm run make
```

The output goes to `out/make`. On Linux this produces a `.deb` and an `.rpm` and needs `dpkg`, `fakeroot` and `rpm` installed.

GitHub Actions builds and releases the Linux packages only. To publish a new release:

1. Bump `version` in `package.json`.
2. Commit and push to the `main` branch.
3. GitHub Actions creates the tag `v<version>` with a GitHub release and attaches the Linux packages to it. A push that does not change the version releases nothing.

## Usage

- Upon starting, the application will display the default webcam feed.
- Use the dropdown menu to switch between available webcams.
- Click the fullscreen button to maximize the video feed.
- Click the Picture-in-Picture button to view the feed in PIP mode.

## Project Structure

- **index.js**: Main file for starting the Electron application.
- **index.html**: Contains the UI for selecting webcam inputs and controlling video features.
- **package.json**: Configuration and scripts for the project.
- **forge.config.js**: Electron Forge packaging configuration.
- **preload.js**: Preload script loaded by the Electron window.

## GitHub Actions

- `.github/workflows/tag.yml` runs on every push to `main`. If the version in `package.json` has no `v<version>` tag yet, it creates the tag and a GitHub release, then calls the Linux build.
- `.github/workflows/linux-release.yml` runs `npm ci` and `npm run make` on Ubuntu and attaches the `.deb` and `.rpm` to the release of the tag. It also runs when a `v*` tag is pushed by hand, and can be started manually as a build-only check (it publishes only when run on a tag).

There are no Windows or macOS builds in CI.

## Dependencies

- **Electron**: Cross-platform desktop application framework.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Acknowledgements

- Icons provided by [Icons8](https://icons8.com).

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

