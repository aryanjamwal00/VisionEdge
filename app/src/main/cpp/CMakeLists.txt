cmake_minimum_required(VERSION 3.22.1)

project("visionedge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 🔹 Change this to your actual OpenCV Android SDK path
set(OpenCV_DIR "C:/Users/aryan/Downloads/opencv-4.12.0-android-sdk/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        ${OpenCV_LIBS})
