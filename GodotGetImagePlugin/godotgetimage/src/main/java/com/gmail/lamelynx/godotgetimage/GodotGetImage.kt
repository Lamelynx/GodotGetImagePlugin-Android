package com.gmail.lamelynx.godotgetimage

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.collection.ArraySet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream


/**
 * Author: Lamelynx
 * Mail: lamelynx@gmail.com
 */

class GodotGetImage(activity: Godot) : GodotPlugin(activity) {

    private val TAG: String = "godot"
    private val REQUEST_GALLERY_IMAGE_ID = 101
    private val REQUEST_CAMERA_CAPTURE_ID = 102
    private val REQUEST_PERMISSION_ID = 1001

    private var tempImage: File? = null
    private var resendPermission = false

    // Options, sets by setOptions()
    private var imgHeight: Int? = null
    private var imgWidth: Int? = null
    private var keepAspect: Boolean? = null
    private var imageQuality: Int = 90

    init {
        Log.d(TAG, "init GodotGetImage")
    }

    override fun getPluginName(): String {
        // Plugin name
        return "GodotGetImage"
    }

    override fun getPluginMethods(): List<String> {
        // Available plugin functions to use in Godot
        return listOf("getGalleryImage", "getGalleryImages", "getCameraImage", "resendPermission", "setOptions")
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = ArraySet()
        signals.add(SignalInfo("image_request_completed", Dictionary::class.java))
        signals.add(SignalInfo("error", String::class.java))
        signals.add(SignalInfo("permission_not_granted_by_user", String::class.java))

        return signals
    }

    fun resendPermission() {
        Log.d(TAG, "Call - resendPermission")
        resendPermission = true
    }

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
        }
    }

    fun getGalleryImage() {
        /**
         * Select single image from gallery
         * Returns godot Dictionary of one image as ByteArray
         */

        Log.d(TAG, "Call - getGalleryImage")

        // Check permission
        if (getPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "image/*"
            activity?.startActivityForResult(intent, REQUEST_GALLERY_IMAGE_ID)
        }
    }

    fun getGalleryImages() {
        /**
         * Select multiple images from gallery
         * Returns godot Dictionary of selected images as ByteArray
         */
        Log.d(TAG, "Call - getGalleryImages")

        // Check permission
        if (getPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "image/*"
            activity?.startActivityForResult(intent, REQUEST_GALLERY_IMAGE_ID)
        }
    }

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
                takePictureIntent.resolveActivity(godot.packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        emitSignal("error", "Could not create camera capture target file")
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            godot,
                            godot.packageName,
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        activity?.startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE_ID)
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
            //val image_list = mutableListOf<String>()
            val imageDict = Dictionary()  // TODO: Godot plugin system does not accept List yet

            if (data.clipData != null) {
                /**
                 * Image selected
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
                 * Single image selected
                 */
                // TODO - Is this 'if' ever being executed?
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
            Log.e(TAG, "Camera image received, resultCode: $resultCode")

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
        bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
        stream.close()
        return stream.toByteArray()
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        /**
         * Uri to bitmap
         * getBitmap is deprecated for >= SDK 28
         * @return Bitmap object
         */

        var bitmap: Bitmap

        if (imgWidth != null && imgHeight != null) {
            // If image width/height is set. Load bitmap only as big as necessary to memory.
            // This does not scale bitmap to the exact size. Need to do another 'exact' scale later on
            bitmap = decodeSampledBitmapFromUri(uri, imgHeight!!, imgWidth!!)
        } else {
            // Load image without any max size. May cause 'out of memory' on big images
            val opt = BitmapFactory.Options()
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888
            val inputImage = godot.contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputImage, null, opt)!!
            inputImage?.close()
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
        Log.d(TAG, "Final image size - width: " + bitmap.width + " height: " + bitmap.height)

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

        if (ContextCompat.checkSelfPermission(
                godot,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Application has not permission: $permission")
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    godot,
                    permission
                )
            ) {
                // User won't grant permission
                Log.d(TAG, "Permission resend: $resendPermission")
                if (resendPermission) {
                    resendPermission = false
                    ActivityCompat.requestPermissions(
                       godot,
                       arrayOf(permission),
                        REQUEST_PERMISSION_ID
                    )
                } else {
                    emitSignal("permission_not_granted_by_user", permission)
                }
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    godot,
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
        val storageDir: File? = godot.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "tmpImage1133", /* prefix */
            ".jpg", /* suffix */
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
            var input: InputStream? = godot.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input, null, this)
            input?.close()

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            input = godot.contentResolver.openInputStream(uri)
            val bitmap: Bitmap = BitmapFactory.decodeStream(input, null, this) as Bitmap
            input?.close()
            bitmap
        }
    }
}