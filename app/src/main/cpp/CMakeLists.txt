cmake_minimum_required(VERSION 3.22.1)
project("echosense_native")

find_package(oboe REQUIRED CONFIG)

# Native library
add_library(echosense_native SHARED
        native-lib.cpp
        AudioEngine.cpp)

# Enable high-level optimizations
target_compile_options(echosense_native PRIVATE -O3)

target_link_libraries(echosense_native
        oboe::oboe
        log)

# --- Unit Tests ---
# Note: These are for local NDK testing if needed
# and verify logic that doesn't depend on Android-specific APIs
add_executable(limiter_test ../../test/cpp/LimiterTest.cpp)
target_include_directories(limiter_test PRIVATE .)
