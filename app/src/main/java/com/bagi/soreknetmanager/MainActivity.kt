package com.bagi.soreknetmanager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import com.bagi.soreknetmanager.databinding.ActivityMainBinding
import com.bagi.soreknetmanager.helpers.DocumentHelper
import com.bagi.soreknetmanager.managers.FirebaseManager
import com.bagi.soreknetmanager.network.RetrofitManager
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


const val SELECT_PICTURES = 890
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var urlLD = MutableLiveData<String>()
    var imagesUri = ArrayList<Uri>()
    var imagesStringPath = ArrayList<String?>()
    var lastIdNews: Int? = null

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainActivity = this
        binding.lifecycleOwner = this
        getNumber()
    }


    @ExperimentalCoroutinesApi
    private fun getNumber() {
        CoroutineScope(IO).launch {
            withContext(IO) {
                FirebaseManager.getLastNumber().collect {
                    lastIdNews = it.toInt()
                }
            }
        }
    }

    fun onClickSetAdvertising() {
        urlLD.value?.let { FirebaseManager.writeAdvertisement(it) }
    }

    fun onClickChooseImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Select images"), SELECT_PICTURES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PICTURES) {
            data?.clipData?.let {
                val count = it.itemCount
                for (i in 0 until count) {
                    imagesStringPath.add(DocumentHelper.getPath(this, it.getItemAt(i).uri))
                }
            }

            data?.data?.let { uri ->
                imagesUri.add(uri)
                imagesStringPath.add(DocumentHelper.getPath(this, uri))
                imagesStringPath[0]?.let { File(it) }
            }
        }

        for (file in imagesStringPath) {
            file?.let {
                val myFile = File(it)
                val multiPart = MultipartBody.Part.createFormData(
                    "image",
                    myFile.name,
                    myFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                )
                GlobalScope.launch {
                    val response = flow {
                        emit(RetrofitManager.instanceServiceApi.postImage(multiPart))
                    }
                    response.catch {
                        Log.i("MainActivity", "error ")
                    }
                    response.collect {
                        Log.i(TAG, it.data.link)
                        uploadNewsToFirebase(it.data.link)
                    }
                }

            }

        }
        imagesStringPath.clear()
    }

    private fun uploadNewsToFirebase(link: String) {
        lastIdNews?.let {
            FirebaseManager.uploadNewsToFirebase(link, it - 1)
            lastIdNews = it - 1
            return
        }
        Toast.makeText(this, "lastIdNews $lastIdNews", Toast.LENGTH_LONG).show()
    }
}
