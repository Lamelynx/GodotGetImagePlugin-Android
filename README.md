GodotGetImagePlugin for Godot 3.2.2+
====================================
____________________________________


Android plugin for Godot 3.2.2 and above.  
Pick image from gallery or take new image from camera.

See GodotExample for more info (Godot 3.2.2).

Installation
============

Follow these instructions for android custom build, [ official documentation](https://docs.godotengine.org/en/stable/getting_started/workflow/export/android_custom_build.html "documentation").

Unzip:  
*release/godotgetimageplugin_for_godot_[your Godot version].zip* to *[your godot project]/android/plugins/*

If there is no GodotGetImagePlugin release for your Godot version, you need to generate new plugin .aar file.  
Follow these instruction: [ official documentation](https://docs.godotengine.org/en/stable/tutorials/plugins/android/android_plugin.html "documentation").

Activate plugin in Godot by enable "Project" -> "Export" -> "Options", "Use Custom Build" and "Godot Get Image" plugin

# Plugin API

It is preferable to set the image size to the maximum desired size before any image requests. This minimize the risk of getting "out of memory" when loading image with unknown sizes.

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
	
```python
e.g.
dict = {
	"image_height" : 1000,
	"image_width" : 600,
	"keep_aspect" : true
}
or
dict = {
	"image_quality" : 40
}
```



Emitted signals
---------------

***image_request_completed***  
Returns a Dictionary with images as PoolByteArray

***permission_not_granted_by_user***   
User declines Android permission request.  
It's a good practice to explain why permission is important and then call resendPermission()

***error***  
Returns any error as string
