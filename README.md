# TCP To Speech Android App

This is an Android application that listens for TCP connections on port 7775 and converts received text to speech.

## Features
- Listens on TCP port 7775 for incoming connections
- Converts received text to speech using Android's TextToSpeech API
- Displays server status and last received message
- Runs as a foreground service for continuous operation

## Prerequisites
- Android Studio
- Android device or emulator running Android 5.0 (API level 21) or higher

## Building the App
1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build the project
4. Run the app on your device or emulator

## Testing the App
1. Start the app on your Android device
2. Tap "Start Server" to start the TCP server
3. Use the provided TestClient.java to send test messages:
   ```bash
   javac TestClient.java
   java TestClient <device_ip> "Hello, this is a test message"
   ```
4. The app should speak the received message and display it on the screen

## Permissions
The app requires the following permissions:
- INTERNET: To establish TCP connections
- FOREGROUND_SERVICE: To run the server as a foreground service

## Port Configuration
The server listens on port 7775 by default. To change the port, modify the `PORT` constant in `TcpServerService.java`.

## Troubleshooting
- Ensure your device and test machine are on the same network
- Check that the port 7775 is not blocked by firewall
- Verify that text-to-speech is enabled on your device
