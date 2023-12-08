GodotGetImage plugin for Godot 4.2+ 
====================================
____________________________________


Android plugin for Godot 4.2+.
Pick one or more images from gallery or capture image from camera.

See demo project [`plugin/demo/`](plugin/demo/) (Godot 4.2.0).

**_NOTE:_** Starting in Godot 4.2, Android plugins built on the v1 architecture are now deprecated. Instead, Godot 4.2 introduces a new Version 2 (v2) architecture for Android plugins. This plugin is from now on build on Godot Plugin v2 system.

More information about v2 architecture: [official documentation] (https://docs.godotengine.org/en/stable/tutorials/platform/android/android_plugin.html "documentation")

Upgrade from Godot plugin v1 to v2 architecture
===============================================

1. Remove GodotGetImageRelease.arr and GodotGetImage.gdap in the folder: *[you project]/android/plugin*

2. Follow installation instructions below

Installation
============

1. If exists, unzip the precompiled release zip in the release folder to your android plugin folder:
*release/godotgetimageplugin_for_godot_[your Godot version].zip* to *[your godot project]/addons/*

2. Activate plugin in Godot by enable Navigate to `Project` -> `Project Settings...` -> `Plugins`, and ensure the plugin "GodotGetImage" is enabled.

Build plugin .aar file
----------------------

If there is no GodotGetImage release for your Godot version, you need to generate new plugin .aar file.  

1. Set correct Godot version by edit the gradle file [`plugin/build.gradle.kts`](plugin/build.gradle.kts):

```
dependencies {
    // Update this to match your Godot engine version
    implementation("org.godotengine:godot:4.2.0.stable")
```

2. Compile the project:

	Open command window and *cd* into plugin root directory and run command below
	
	* Windows:
	
		gradlew.bat assemble
		
	* Linux:
	
		./gradlew assemble
	
3. On successful completion of the build, the output files can be found in
  [`plugin/demo/addons/GodotGetImage`](plugin/demo/addons/GodotGetImage)

# Plugin API

It is preferable to set the image size to the maximum desired size before any image requests. This minimize the risk of getting "out of memory" when loading image with unknown sizes.

~~When loading image buffer into your godot image, don't forget ***yield(get_tree(), "idle_frame")***. Otherwise you would get a black image.~~

**_NOTE:_** "idle_frame" (or "process_frame" as it was called in Godot 4) is NOT necessary in Godot 4.x

Permissions
-----------

The plugin should handle all permissions that is neede. If any problem set these permission in Godot editor -> Project -> Export window:

*Read External Storage*

*Read Media Images*

**_NOTE:_** As of Godot 4.2 this permission does not exists in the editor.

*Camera*
	
Methods
-------

***getGalleryImage()***  
Select one image from gallery

***getGalleryImages()***  
Select multiple images from gallery

***getCameraImage()***  
Capture image from camera

***resendPermission()***  
If user has declined permission this needs to be called for a new permission is requested.

***setOptions(*** *Dictionary with options* ***)***  
If you would like a specific size of the images, it can set via this feature.  
This will apply to all images until options are set again or ***setOptions(*** *{ }* ***)*** is called with an empty dictionary.

*This will not make the image larger than the original, in which case the original size will be kept.*

#### Available options:
* *"image_height"* (Int): Sets maximum image height
* *"image_width"* (Int): Sets maximum image width
* *"keep_aspect"* (Bool): Keep aspect ratio or not
* *"image_quality"* (Int 0-100): Sets image compression quality, default is 90. 100 is best quality with least compression.
* *"image_format"* (String): Set the compression format returned by plugin (supported formats: *"jpg"* , *"png"*). Default "jpg".
* *"auto_rotate_image"* (Bool): Plugin will try to set correct orientation of the image. This is not 100% but will mostly return a correct oriented camera image.
* *"use_front_camera"* (Bool): Plugin will try to use front camera.

**_NOTE:_** Remember to load correct format in your code: ***load_jpg_from_buffer()*** or ***load_png_from_buffer()***
	
```python
e.g.
dict = {
	"image_height" : 1000,
	"image_width" : 600,
	"keep_aspect" : true,
	"auto_rotate_image" : true
}
or
dict = {
	"image_quality" : 40,
	"image_format" : "png"
}
```



Emitted signals
---------------

***image_request_completed***  
Returns a Dictionary with images as PoolByteArray

**_NOTE:_** If non supported image is selected this will return null value

***permission_not_granted_by_user***   
User declines Android permission request.  
It's a good practice to explain why permission is important and then call resendPermission()

***error***  
Returns any error as string

# Donation
If you like this plugin and really wish to make a donation. 
Feel free to make a donation to [ The Children's Heart Fund](https://mitt.hjartebarnsfonden.se/14901 "Hjärtebarnsfonden").
