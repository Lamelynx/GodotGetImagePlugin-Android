package com.gmail.lamelynx.godotgetimage

import android.util.Log
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.isPhotoPickerAvailable
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.SignalInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream


/**
 * Author: Lamelynx
 * Mail: lamelynx@gmail.com
 */

// Make this plugin compatible to other plugins that is using FileProvider()
// Is used in AndroidManifest.xml
class GGIFileProvider : FileProvider()


class GodotGetImage(godot: Godot): GodotPlugin(godot) {

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = mutableSetOf()
        signals.add(SignalInfo("image_request_completed", Dictionary::class.java))
        signals.add(SignalInfo("error", String::class.java))
        signals.add(SignalInfo("permission_not_granted_by_user", String::class.java))

        return signals
    }

    private val TAG: String = "godot"
    private val REQUEST_GALLERY_IMAGE_ID = 101
    private val REQUEST_CAMERA_CAPTURE_ID = 102
    private val REQUEST_PERMISSION_ID = 1001

    private var tempImage: File? = null
    private var resendPermission = false
    private lateinit var myPhotoPicker:ActivityResultLauncher<PickVisualMediaRequest>

    // Options, sets by setOptions()
    private var imgHeight: Int? = null
    private var imgWidth: Int? = null
    private var keepAspect: Boolean? = null
    private var imageQuality: Int = 90
    private var imageFormat: String = "jpg"
    private var autoRotateImage: Boolean = false
    private var useFrontCamera: Boolean = false
    // Available image format compression
    private var supportedImageFormats:List<String> = listOf("jpg", "png")
    private var usePhotoPicker: Boolean = true

    /*
    init {
        Log.d(TAG, "init GodotGetImage")
        val tmp = PhotoPicker()
        tmp.test()


        if (isPhotoPickerAvailable(activity!!)) {
            Log.d(TAG, "PhotoPicker is available on this device")

            val tmp = androidx.activity.result.ActivityResult
            // Registers a photo picker activity launcher in single-select mode.
            myPhotoPicker = registerForActivityResult(PickVisualMedia()) { uri ->
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d(TAG, "Selected URI: $uri")
                } else {
                    Log.d(TAG, "No media selected")
                }
            }

        } else {
            // If photo picker is no available use old capturing method regardless what is set
            // through setOptions() from Godot app.
            usePhotoPicker = false
            Log.d(TAG, "PhotoPicker is NOT available on this device")
        }
        


    }*/

    @UsedByGodot
    private fun resendPermission() {
        Log.d(TAG, "Call - resendPermission")
        resendPermission = true
    }

    @UsedByGodot
    fun setOptions(opt: Dictionary) {
        /**
         * Set options for all images
         */

        if (opt.isEmpty()) {
            Log.d(TAG, "Call - setOptions, no option specified. Set to default")
            // Set all options to default
            imgWidth = null
            imgHeight = null
            keepAspect = null
            imageQuality = 90
            imageFormat = "jpg"
            autoRotateImage = false
            useFrontCamera = false
            usePhotoPicker = true
        } else {
            Log.d(TAG, "Call - setOptions, Options:" + opt.keys + ", Values: " + opt.values)
            if ("image_width" in opt.keys) {
                imgWidth = opt["image_width"] as Int
            }
            if ("image_height" in opt.keys) {
                imgHeight = opt["image_height"] as Int
            }
            if ("keep_aspect" in opt.keys) {
                keepAspect = opt["keep_aspect"] as Boolean
            }
            if ("image_quality" in opt.keys) {
                imageQuality = opt["image_quality"] as Int
            }
            if ("image_format" in opt.keys) {

                if (supportedImageFormats.contains(opt["image_format"] as String)) {
                        imageFormat = opt["image_format"] as String
                } else {
                    emitSignal("error", "Image format $opt['image_format'] is not supported!")
                }
            }
            if ("auto_rotate_image" in opt.keys) {
                autoRotateImage = opt["auto_rotate_image"] as Boolean
            }
            if ("use_front_camera" in opt.keys) {
                useFrontCamera = opt["use_front_camera"] as Boolean
            }
            if ("use_photo_picker" in opt.keys) {
                usePhotoPicker = opt["use_photo_picker"] as Boolean
            }
        }
    }

    @UsedByGodot
    fun getGalleryImage() {
        /**
         * Select single image from gallery
         * Returns godot Dictionary of one image as ByteArray
         */

        Log.d(TAG, "Call - getGalleryImage")
        /*
        if (usePhotoPicker){
            myPhotoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            return
        }
        */

        // Android 13 changed permissions to READ_MEDIA_STORAGE
        val permissionToRequest =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        // Check permission
        if (getPermission(permissionToRequest)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "image/*"
            activity?.startActivityForResult(intent, REQUEST_GALLERY_IMAGE_ID)
        }
    }

    @UsedByGodot
    fun getGalleryImages() {
        /**
         * Select multiple images from gallery
         * Returns godot Dictionary of selected images as ByteArray
         */
        Log.d(TAG, "Call - getGalleryImages")

        // Android 13 changed permissions to READ_MEDIA_STORAGE
        val permissionToRequest =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        // Check permission
        if (getPermission(permissionToRequest)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "image/*"
            activity?.startActivityForResult(intent, REQUEST_GALLERY_IMAGE_ID)
        }
    }

    @UsedByGodot
    fun getCameraImage() {
        /**
         * Capture image from camera
         * Returns godot Dictionary with one images as ByteArray
         */
        Log.d(TAG, "Call - getCameraImage")

        // Check permission
        if (getPermission(Manifest.permission.CAMERA)) {

            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                if (activity != null) {
                    takePictureIntent.resolveActivity(activity!!.packageManager).also {
                        // Create the File where the photo should go
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            // Error occurred while creating the File
                            emitSignal("error", "Could not create image")
                            null
                        }

                        // Continue only if the File was successfully created
                        photoFile?.also {
                            val photoURI: Uri = FileProvider.getUriForFile(
                                activity!!.applicationContext,
                                activity!!.packageName + ".ggi_FileProvider",
                                it
                            )
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                            if (useFrontCamera) {
                                // Try to open camera application with front camera

                                // Extras for displaying the front camera on most devices
                                takePictureIntent.putExtra("com.google.assistant.extra.USE_FRONT_CAMERA", true)
                                takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                                takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1)

                                // Extras for displaying the front camera on Samsung
                                takePictureIntent.putExtra("camerafacing", "front")
                                takePictureIntent.putExtra("previous_mode", "Selfie")

                                // TODO this part is not tested
                                val targetPackage = takePictureIntent.resolveActivity(activity!!.packageManager)
                                //Log.d(TAG, targetPackage.toString())
                                if (targetPackage?.toString()?.contains("honor", ignoreCase = true) == true) {
                                    // Extras for displaying the front camera on Honor
                                    takePictureIntent.putExtra("default_camera", "1")
                                    takePictureIntent.putExtra("default_mode", "com.hihonor.camera2.mode.photo.PhotoMode")
                                } else {
                                    // Extras for displaying the front camera on Huawei
                                    takePictureIntent.putExtra("default_camera", "1")
                                    takePictureIntent.putExtra("default_mode", "com.huawei.camera2.mode.photo.PhotoMode")
                                }
                            }

                            activity?.startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE_ID)
                        }
                    }
                }
            }
        }
    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /**
         * This will be called by Godot class
         */
        if (requestCode == REQUEST_GALLERY_IMAGE_ID && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Received image from gallery")

            val imageDict = Dictionary()  // TODO: Godot plugin system does not accept List yet

            if (data.clipData != null) {
                /**
                 * One or more images is selected
                 */

                val images = data.clipData
                val count:Int = images?.itemCount ?: 0

                for (i in 0 until count) {
                    val imageUri: Uri? = images?.getItemAt(i)?.uri
                    val bitmap = imageUri?.let { loadBitmap(it) }
                    imageDict[i.toString()] = bitmap?.let { bitmapToByteArray(it) }
                }
            } else if (data.data != null) {
                /**
                 * Single image is selected
                 */
                Log.d(TAG, "Single image selected")
                val imageUri: Uri? = data.data
                val bitmap = imageUri?.let { loadBitmap(it) }
                imageDict["0"] = bitmap?.let { bitmapToByteArray(it) }
            }

            Log.d(TAG, "Number of images selected: " + imageDict.size)
            emitSignal("image_request_completed", imageDict)

        } else if (requestCode == REQUEST_CAMERA_CAPTURE_ID && resultCode == Activity.RESULT_OK && tempImage != null) {
            /**
             * Image received from camera
             */
            Log.d(TAG, "Camera image received, resultCode: $resultCode")

            val imageFile: File = tempImage as File

            val imageUri: Uri? = Uri.fromFile(imageFile)
            val bitmap = imageUri?.let { loadBitmap(it) }

            imageUri?.path?.let { Log.d(TAG, "Received images from camera: $it, resultCode: $resultCode") }

            val imageDict = Dictionary()
            imageDict["0"] = bitmap?.let { bitmapToByteArray(it) }

            // Delete temporary file
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    Log.d(TAG, "File deleted: " + imageFile.path)
                } else {
                    Log.d(TAG, "File not deleted: " + imageFile.path)
                }
            }

            tempImage = null  // Reset image file

            emitSignal("image_request_completed", imageDict)
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap):ByteArray {
        /**
         * Convert Bitmap to ByteArray
         * @return ByteArray object
         */
        val stream = ByteArrayOutputStream()
        if (imageFormat == "jpg") {
            bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
        } else if (imageFormat == "png") {
            bitmap.compress(Bitmap.CompressFormat.PNG, imageQuality, stream)
        }

        stream.close()
        return stream.toByteArray()
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        /**
         * Uri to bitmap
         * getBitmap is deprecated for >= SDK 28
         * @return Bitmap object
         */


        var bitmap: Bitmap?

        if (imgWidth != null && imgHeight != null) {

            // If image width/height is set. Load bitmap only as big as necessary to memory.
            // This does not scale bitmap to the exact size. Need to do another 'exact' scale later on
            bitmap = decodeSampledBitmapFromUri(uri, imgHeight!!, imgWidth!!)

        } else {
            // Load image without any max size. May cause 'out of memory' on big images
            val opt = BitmapFactory.Options()
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888
            val inputImage = activity?.contentResolver?.openInputStream(uri)

            bitmap = BitmapFactory.decodeStream(inputImage, null, opt)

            inputImage?.close()
        }

        // If a unsupported bitmap format (ex. svg) is selected, bitmap is null
        if (bitmap != null) {
            if (autoRotateImage) {
                bitmap = rotateImageIfRequired(activity, bitmap, uri)
            }

            if (imgHeight != null && imgWidth != null && keepAspect != null) {
                /**
                 * Scale image
                 */
                Log.d(TAG, "Scale image to $imgWidth * $imgHeight , keep aspect: $keepAspect")
                bitmap = if (keepAspect as Boolean) {
                    resizeBitmap(bitmap, imgHeight as Int)
                } else {
                    createScaledBitmap(bitmap, imgWidth as Int, imgHeight as Int, false)
                }
            }
            Log.d(TAG, "Final image size - width: " + bitmap!!.width + " height: " + bitmap.height)
        }
        return bitmap
    }

    private fun resizeBitmap(originalBitmap: Bitmap, widthOrHeight: Int): Bitmap {
        /**
         * Resize bitmap but keep aspect ratio
         *
         * @return original or scaled bitmap
         */
        try {
            if (originalBitmap.height >= originalBitmap.width) {
                if (originalBitmap.height <= widthOrHeight) {
                    // Original image is smaller then max height
                    return originalBitmap
                }

                val aspectRatio = originalBitmap.width.toDouble() / originalBitmap.height.toDouble()
                val targetWidth = (widthOrHeight * aspectRatio).toInt()
                return createScaledBitmap(originalBitmap, targetWidth, widthOrHeight, false)
            } else {
                if (originalBitmap.width <= widthOrHeight) {
                    // Original image is smaller then max width
                    return originalBitmap
                }

                val aspectRatio = originalBitmap.height.toDouble() / originalBitmap.width.toDouble()
                val targetHeight = (widthOrHeight * aspectRatio).toInt()

                return createScaledBitmap(originalBitmap, widthOrHeight, targetHeight, false)
            }
        } catch (e: Exception) {
            return originalBitmap
        }
    }

    private fun getPermission(permission: String): Boolean {
        /**
         * Ask for permission to user.
         * If user decline a signal is emitted to Godot.
         *
         * @return true if permission is already granted
         */
        var ret = false

        val perm = ContextCompat.checkSelfPermission(activity!!, permission)
        if ( perm != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Application has not permission: $permission")

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission)) {
                // User won't grant permission
                Log.d(TAG, "Permission resend: $resendPermission")
                if (resendPermission) {
                    resendPermission = false
                    ActivityCompat.requestPermissions(
                        activity!!,
                       arrayOf(permission),
                        REQUEST_PERMISSION_ID
                    )
                } else {
                    emitSignal("permission_not_granted_by_user", permission)
                }
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    activity!!,
                    arrayOf(permission),
                    REQUEST_PERMISSION_ID
                )
            }
        } else {
            // Permission is already granted
            ret = true
        }
        return ret
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "tmpImage1133", /* prefix */
            ".$imageFormat", /* suffix */
            storageDir /* directory */
        ).apply {
            // Store path to use in onMainActivityResult
            tempImage = this
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun decodeSampledBitmapFromUri(
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            // Preload without actually load image
            inJustDecodeBounds = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
            var input: InputStream? = activity?.contentResolver?.openInputStream(uri)
            BitmapFactory.decodeStream(input, null, this)
            input?.close()

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            input = activity?.contentResolver?.openInputStream(uri)
            val bitmap: Bitmap = BitmapFactory.decodeStream(input, null, this) as Bitmap
            input?.close()
            bitmap
        }
    }

    /**
     * Solution based on https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    @Throws(IOException::class)
    private fun rotateImageIfRequired(context: Context?, img: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context?.contentResolver?.openInputStream(selectedImage)
        val ei: ExifInterface

        if (Build.VERSION.SDK_INT > 23)
            ei = ExifInterface(input!!)
        else
            ei = ExifInterface(selectedImage.path!!)

        if (input != null) {
            input.close()
        }

        val orientation: Int =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        Log.d(TAG, "Rotate image $degree degrees")
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}