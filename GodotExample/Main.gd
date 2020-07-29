extends Node2D

var plugin
var plugin_name = "GodotGetImage"


# Called when the node enters the scene tree for the first time.
func _ready():
	if Engine.has_singleton(plugin_name):
		plugin = Engine.get_singleton(plugin_name)
	else:
		print("Could not load plugin: ", plugin_name)

	if plugin:
		plugin.connect("image_request_completed", self, "_on_image_request_completed")
		plugin.connect("error", self, "_on_error")
		plugin.connect("permission_not_granted_by_user", self, "_on_permission_not_granted_by_user")

func _on_ButtonGallery_pressed():
	if plugin:
		plugin.getGalleryImage()
	else:
		print(plugin_name, " plugin not loaded!")

func _on_ButtonCamera_pressed():
	if plugin:
		plugin.getCameraImage()
	else:
		print(plugin_name, " plugin not loaded!")

func _on_image_request_completed(path):
	"""  This function is working but no image is displayed. It does store a copy in extrenaldir folder """
	var image = Image.new()
	var err = image.load(path)
	#var err = image.load("/storage/emulated/0/Android/data/org.godotengine.godotexample/files/images/new_dir/image.png")
	if err != OK:
		print("Could not load image: ", path)
	
	var texture = ImageTexture.new()
	texture.create_from_image(image)
	find_node("Image").texture = texture
	
	# Proof that image is recieved
	image.save_png("/storage/emulated/0/Android/data/org.godotengine.godotexample/files/selected_image.png")
	#print(OS.getExternalFilesDir)
	
func _on_error(e):
	print("Error: ", e)
	
func _on_permission_not_granted_by_user(permission):
	var dialog = get_node("AcceptDialog")
	dialog.window_title = "Permission necessary"
	var permission_text = permission.capitalize().split(".")[-1]
	dialog.dialog_text = permission_text + "\n permission is necessary"
	dialog.show()
	
	# Set the plugin to get user permission again
	plugin.resendPermission()
	


func _on_ButtonGalleryMulti_pressed(path):
	pass # Replace with function body.
