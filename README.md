# Video Input Monitor

Video Input Monitor is a cross-platform Electron application that allows users to monitor webcam feeds with a clean and intuitive interface. The application includes fullscreen and Picture-in-Picture (PIP) modes, making it ideal for multi-tasking while using webcam feeds.

## Features

- **Webcam Selection**: Easily switch between multiple webcam devices connected to your system.
- **Fullscreen Mode**: View the video feed in fullscreen mode for an immersive experience.
- **Picture-in-Picture (PIP)**: Keep the video feed visible in a floating window while working on other tasks.
- **Automatic Controls Hide**: The controls automatically hide after 5 seconds of inactivity for an unobstructed view.

## Getting Started

### Prerequisites

- **Node.js** (v18 or later)
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

This project includes a GitHub Action workflow to generate executables for Windows, macOS, and Linux. To create a new release:

1. Commit and push your changes to the `main` branch.
2. GitHub Actions will automatically build the project and create executables for each platform as part of the release process.

## Usage

- Upon starting, the application will display the default webcam feed.
- Use the dropdown menu to switch between available webcams.
- Click the fullscreen button to maximize the video feed.
- Click the Picture-in-Picture button to view the feed in PIP mode.

## Project Structure

- **main.js**: Main file for starting the Electron application.
- **index.html**: Contains the UI for selecting webcam inputs and controlling video features.
- **package.json**: Configuration and scripts for the project.
- **preload.js** (optional): Preloading script for secure context handling.

## GitHub Actions

The GitHub Action workflow (`.github/workflows/build.yml`) automatically builds the application for Windows, macOS, and Linux when changes are pushed to the `main` branch, and creates a new release with the executables.

## Dependencies

- **Electron**: Cross-platform desktop application framework.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Acknowledgements

- Icons provided by [Icons8](https://icons8.com).

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

