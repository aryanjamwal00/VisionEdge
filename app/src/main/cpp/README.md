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

