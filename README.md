GodotGetImagePlugin for Godot 3.2.2+
====================================
____________________________________


Android plugin for Godot 3.2.2 and above.  
Pick one or more images from gallery or capture image from camera.

See GodotExample for more info (Godot 3.3).

Installation
============

Follow these instructions for android custom build, [ official documentation](https://docs.godotengine.org/en/stable/getting_started/workflow/export/android_custom_build.html "documentation").

1. If exists, unzip the precompiled release zip in the release folder to your android plugin folder:
*release/godotgetimageplugin_for_godot_[your Godot version].zip* to *[your godot project]/android/plugins/*

2. Activate plugin in Godot by enable "Project" -> "Export" -> "Options", "Use Custom Build" and "Godot Get Image" plugin

Generate plugin .aar file
-------------------------

If there is no GodotGetImagePlugin release for your Godot version, you need to generate new plugin .aar file.  
Follow these instruction: [ official documentation](https://docs.godotengine.org/en/stable/tutorials/plugins/android/android_plugin.html "documentation").

In short follow these steps:

1. Download [ AAR library for Android plugins](https://godotengine.org/download/windows "Godot download").

2. Copy .aar file to *GodotGetImagePlugin/godot-lib.release/* and rename it to *godot-lib.release.aar*

3. Compile the project:

	Open command window and *cd* into *GodotGetImagePlugin* and run command below
	
	* Windows:
	
		gradlew.bat assembleRelease
		
	* Linux:
	
		./gradlew assembleRelease
	
4. Copy the newly created .aar file to your plugin directory:

*/GodotGetImagePlugin/godotgetimage/build/outputs/aar/GodotGetImage.release.aar* to *[your godot project]/android/plugins/*

(don't forget to also copy *GodotGetImage.gdap* from any release zip to *[your godot project]/android/plugins/*)


# Plugin API

It is preferable to set the image size to the maximum desired size before any image requests. This minimize the risk of getting "out of memory" when loading image with unknown sizes.

When loading image buffer into your godot image, don't forget ***yield(get_tree(), "idle_frame")***. Otherwise you would get a black image.

From the GodotExample:

```python
func _on_image_request_completed(dict):
	""" Returns Dictionary of PoolByteArray """
	var count = 0
	for img_buffer in dict.values():
		count += 1
		var image = Image.new()
		
		# Use load format depending what you have set in plugin setOption()
		var error = image.load_jpg_from_buffer(img_buffer)
		#var error = image.load_png_from_buffer(img_buffer)
		
		if error != OK:
			print("Error loading png/jpg buffer, ", error)
		else:
			print("We are now loading texture... ", count)
			yield(get_tree(), "idle_frame")
			var texture = ImageTexture.new()
			texture.create_from_image(image, 0)
			get_node("VBoxContainer/Image").texture = texture
```			
			
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
	
**_NOTE:_** Remember to load correct format in your code: ***load_jpg_from_buffer()*** or ***load_png_from_buffer()***
	
```python
e.g.
dict = {
	"image_height" : 1000,
	"image_width" : 600,
	"keep_aspect" : true
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

***permission_not_granted_by_user***   
User declines Android permission request.  
It's a good practice to explain why permission is important and then call resendPermission()

***error***  
Returns any error as string

# Donation
If you like this plugin and really wish to make a donation. 
Feel free to make a donation to [ The Children's Heart Fund](https://mitt.hjartebarnsfonden.se/14901 "Hjärtebarnsfonden").
