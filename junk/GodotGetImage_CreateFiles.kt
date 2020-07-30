package com.gmail.lamelynx.godotgetimage

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.collection.ArraySet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import java.io.*


/**
 * Author: Lamelynx
 * Mail: lamelynx@gmail.com
 */

class GodotGetImage_CreateFiles(activity: Godot) : GodotPlugin(activity) {

    private val TAG: String = "godot"
    private val GET_GALLERY_IMAGE_CODE = 101
    private val GET_GALLERY_IMAGES_CODE = 102
    private val GET_CAMERA_IMAGE_CODE = 103
    private val PERMISSION_REQUEST_CODE = 1001

    private var resendPermission = false

    init {
        Log.d(TAG, "init GodotGetImage")
    }

    override fun getPluginName(): String {
        // Plugin name
        return "GodotGetImage"
    }

    override fun getPluginMethods(): List<String> {
        // Available plugin functions to use in Godot
        //return Arrays.asList("getGalleryImage", "getGalleryImages", "getCameraImage", "resendPermission")
        return listOf("getGalleryImage", "getGalleryImages", "getCameraImage", "resendPermission")
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = ArraySet()
        signals.add(SignalInfo("image_request_completed", String::class.java))
        signals.add(SignalInfo("image_request_completed_list", Dictionary::class.java))
        signals.add(SignalInfo("image_request_completed_dict", Dictionary::class.java))
        signals.add(SignalInfo("error", String::class.java))
        signals.add(SignalInfo("permission_not_granted_by_user", String::class.java))
        
        return signals
    }

    fun resendPermission() {
        resendPermission = true
    }

    fun getGalleryImage() {
        /* Select single image from gallery */
        Log.d(TAG, "Call - getGalleryImage")

        // Check permission
        if (getPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // TODO Onödig?
            intent.type = "image/*"
            activity?.startActivityForResult(intent, GET_GALLERY_IMAGE_CODE)
        }
    }

    fun getGalleryImages() {
        /* Select multiple images from gallery */
        Log.d(TAG, "Call - getGalleryImages")

        // Check permission
        if (getPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // TODO Onödig?
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "image/*"
            activity?.startActivityForResult(intent, GET_GALLERY_IMAGES_CODE)
        }
    }

    fun getCameraImage() {
        Log.d(TAG, "Call - getCameraImage")

        // Check permission
        if (getPermission(Manifest.permission.CAMERA)) {
            // TODO
            Log.d(TAG, "Call - getCameraImage")
        }
    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // This will be called by Godot class
        if (requestCode == GET_GALLERY_IMAGE_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Log.e(TAG, "Gallery image received, resultCode: $resultCode")

            val imageUri: Uri? = data.data
            val bitmap = imageUri?.let { getBitmap(it) }

            // Put selected image to internal storage or path
            val dirPath = File(godot.filesDir, "images")
            dirPath.mkdirs()
            val file = File(dirPath, "image.jpg")
            if (bitmap != null) {
                storeImage(bitmap, file)
            }

            emitSignal("image_request_completed", file.toString())

        }  else if (requestCode == GET_GALLERY_IMAGES_CODE && resultCode == Activity.RESULT_OK && data != null) {
            //val image_list = mutableListOf<String>()
            val image_dict = Dictionary()  // TODO: Godot plugin system does not accept List yet
            if (data.clipData != null) {
                val images = data.clipData
                val count:Int = images?.itemCount ?: 0

                for (i in 0 until count) {
                    val imageUri: Uri? = images?.getItemAt(i)?.uri
                    val imagePath = imageUri.toString()
                    Log.e(TAG, "Path $i : $imagePath")


                    val bitmap = imageUri?.let { getBitmap(it) }
                    // Convert to bytearray and return it with signal to Godot
                    val stream = ByteArrayOutputStream()
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val image = stream.toByteArray()
                    stream.close()
                    image_dict[i.toString()] = image
                    Log.e(TAG, "dfdfdfd $i : " + image.size)
                }
            } else if (data.getData() != null) {

                val imageUri: Uri? = data.data
                val bitmap = imageUri?.let { getBitmap(it) }
                // Put selected image to internal storage or path
                val dirPath = File(godot.filesDir, "images")
                dirPath.mkdirs()
                val file = File(dirPath, "image.jpg")
                if (bitmap != null) {
                    storeImage(bitmap, file)
                }

                //image_list.add(file.toString())
                //image_dict["0"] = file.toString()
                Log.e("godot", "Path :" + file.toString())

            }
            //Log.d(TAG, "Number of images: " + image_list.size)
            //emitSignal("image_request_completed_list", image_list)
            Log.d(TAG, "Number of images: " + image_dict.size)
            emitSignal("image_request_completed_dict", image_dict)

        } else if (requestCode == GET_CAMERA_IMAGE_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Log.e(TAG, "Camera image received, resultCode: $resultCode")
            // TODO complete

            emitSignal("image_request_completed", "Uri path TODO ----99")
        }
    }

    fun getBitmap(uri: Uri):Bitmap {
        // getBitmap is deprecated for >= SDK 28
        return uri.let {
            if (Build.VERSION.SDK_INT < 28) {
                Log.e(TAG, "SDK < 28")
                MediaStore.Images.Media.getBitmap(
                    godot.contentResolver,
                    it
                )
            } else {
                Log.e(TAG, "SDK >= 28")
                val source = ImageDecoder.createSource(godot.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    fun getPermission(permission: String): Boolean {
        /* Returns true if permission is already granted */
        var ret = false

        if (ContextCompat.checkSelfPermission(
                godot,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "No permission to read from external storage")
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    godot,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                // User won't grant permission
                if (resendPermission) {
                    resendPermission = false
                    ActivityCompat.requestPermissions(
                       godot,
                       arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_CODE
                    );
                } else {
                    emitSignal("permission_not_granted_by_user", permission.toString())
                }
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    godot,
                    arrayOf(permission),
                    PERMISSION_REQUEST_CODE
                );
            }
        } else {
            // Permission is already granted
            ret = true
        }
        return ret
    }

    private fun storeImage(image: Bitmap, dst: File) {
        try {
            val fos = FileOutputStream(dst)
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: " + e.message)
        }
    }
}