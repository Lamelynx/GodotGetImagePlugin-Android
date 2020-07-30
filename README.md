# GodotGetImagePlugin for Godot 3.2.2+
======================================

Godot plugin to pick image from gallery or capture image from camera

Installation
------------

* Follow these instructions for custum build:
    https://docs.godotengine.org/en/stable/getting_started/workflow/export/android_custom_build.html

* Copy GodotGetImagePlugin\godotgetimage\build\outputs\aar\GodotGetImage.release.aar to [your godot project]\android\plugins

API 
---
* Methods
    ```python
    getGalleryImage()
    ```
    Select one image from gallery
    
    ```python
    getGalleryImages()
    ```
    Select multiple images from gallery
    
    ```python
    getCameraImage()
    ```
    Get image from camera
    
    ```python
    resendPermission()
    ```
    If user decline permission request, call this function before any of the abowe.
    It's usually a good practice to explain to user why you need the permission.

* Signals
image_request_completed
Returns a Dictionary of image as PoolByteArray

* permission_not_granted_by_user

* error
Returns any error as string
