cmake_minimum_required(VERSION 3.22.1)
project("echosense_native")

find_package(oboe REQUIRED CONFIG)

add_library(echosense_native SHARED
        native-lib.cpp
        AudioEngine.cpp)

target_link_libraries(echosense_native
        oboe::oboe
        log)