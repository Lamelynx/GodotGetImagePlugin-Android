# GodotGetImagePlugin for Godot 3.2.2+


Android Godot plugin for picking image from gallery or capture image from camera.

See GodotExample for more info (Godot 3.2.2).

### Installation

* Follow these instructions for android custum build, [ official documentation](https://docs.godotengine.org/en/stable/getting_started/workflow/export/android_custom_build.html "documentation").

* For Godot 3.2.2 unzip:
>release/godotgetimageplugin_for_godot_3.2.2.zip to [your godot project]/android/plugins/ directory

* Activate plugin by turn on Project -> Export -> Options, "Use Custom Build" and "Godot Get Image" plugin

### API  Methods

> getGalleryImage()

Select one image from gallery

> getGalleryImages()

 Select multiple images from gallery

>getCameraImage()

Get image from camera

>resendPermission()

If user decline permission request, call this function before any of the above.
It's usually a good practice to explain to user why you need the permission.

### API Signals
>image_request_completed

Returns a Dictionary of image as PoolByteArray

>permission_not_granted_by_user

When user won't grant Android permission

>error

Returns any error as string
