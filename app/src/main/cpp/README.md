# VisionEdge 🎯

**VisionEdge** is an Android + Native C++ (NDK) based project that combines Kotlin, C++, OpenCV, and OpenGL to perform **real-time image processing** and **edge detection** directly on mobile devices.

---

## 🚀 Project Overview

The goal of this project is to:
- Capture live camera frames using Android’s Camera API.
- Process frames in native C++ using OpenCV (via JNI).
- Render the processed output using OpenGL for visualization.
- Provide a modular web viewer to preview processed frames.

---

## 🧱 Tech Stack

| Layer | Technology |
|-------|-------------|
| Frontend (App) | Kotlin + Android SDK |
| Native Processing | C++17 (via JNI) |
| Vision Library | OpenCV |
| Rendering | OpenGL Shaders |
| Web Interface | TypeScript + HTML |
| Build Tools | Gradle + CMake |

---

## ⚙️ Current Progress (as of Day 1)

- ✅ Android Studio Native-C++ Project Created
- ✅ JNI bridge verified with `stringFromJNI()`
- ✅ XML layout updated (`sample_text` view)
- ✅ Native logging working (`VisionEdgeNative` log tag)
- 🔜 Next: Integrate OpenCV & camera module

---

## 📂 Project Structure

## 🎮 Runtime Controls

The VisionEdge app supports real-time filter switching:

- **Original** – shows the raw camera feed.
- **Grayscale** – displays frames converted to gray via OpenCV.
- **Edges** – shows Canny edge detection over the live camera feed.

The current mode is always visible at the top-left overlay label (e.g., `Mode: EDGE`).

### 📸 Frame Capture

- Tap the **Capture** button to save the currently processed frame.
- Captured images are stored under the app's pictures directory:

  `Android/data/com.example.visionedge/files/Pictures/visionedge_<timestamp>.png`

This is implemented using `getExternalFilesDir(Environment.DIRECTORY_PICTURES)` so no additional storage permission is needed on recent Android versions.

## 🧪 How to Demo

1. Install the app via Android Studio on an emulator or physical device.
2. Grant **Camera** permission on first launch.
3. Switch between **Original / Grayscale / Edges** using the bottom controls.
4. Tap **Capture** to persist any interesting processed frame to disk.
