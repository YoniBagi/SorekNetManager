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
import com.bagi.soreknetmanager.helpers.ConvertStringToHtml
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


const val SELECT_PICTURES = 890
const val SELECT_PICTURE_LONG_NEWS = 990
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var urlLD = MutableLiveData<String>()
    var imagesUri = ArrayList<Uri>()
    var imagesStringPath = ArrayList<String?>()
    var lastIdNewsLD = MutableLiveData<Int>()
    var contentNewsLd = MutableLiveData<String>()
    var dateNewsLd = MutableLiveData<String>()
    var titleNewsLd = MutableLiveData<String>()
    var pbVisibilityLd = MutableLiveData<Boolean>()

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainActivity = this
        binding.lifecycleOwner = this
        getNumber()
        getDate()
    }

    private fun getDate() {
        val cal = Calendar.getInstance()
        dateNewsLd.value = SimpleDateFormat("dd-MM-yy", Locale.ENGLISH).format(cal.time)
    }


    @ExperimentalCoroutinesApi
    private fun getNumber() {
        CoroutineScope(IO).launch {
            withContext(IO) {
                FirebaseManager.getLastNumber().collect {
                    lastIdNewsLD.postValue(it.toInt())
                }
            }
        }
    }

    fun onClickSetAdvertising() {
        urlLD.value?.let { FirebaseManager.writeAdvertisement(it) }
    }

    fun onClickChooseImages() {
        selectImageFromDevice(SELECT_PICTURES)
    }

    private fun selectImageFromDevice(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Select images"), requestCode)
    }

    fun onClickUploadLongNews() {
        selectImageFromDevice(SELECT_PICTURE_LONG_NEWS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pbVisibilityLd.value = true
        if (resultCode == Activity.RESULT_OK && (requestCode == SELECT_PICTURES || requestCode == SELECT_PICTURE_LONG_NEWS)) {
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
                        pbVisibilityLd.postValue(false)
                    }
                    response.collect { imgurResponse ->
                        uploadNewsToFirebase(
                            titleNewsLd.value,
                            dateNewsLd.value,
                            contentNewsLd.value,
                            imgurResponse.data.link,
                            requestCode)
                        pbVisibilityLd.postValue(false)
                    }
                }

            }

        }
        imagesStringPath.clear()
    }

    private fun uploadNewsToFirebase(
        title: String? = "",
        date: String? = "",
        content: String? = "",
        link: String,
        requestCode: Int
    ) {
        lastIdNewsLD.value?.let {
            when (requestCode){
                SELECT_PICTURES -> FirebaseManager.uploadNewsToFirebase(link, it - 1)
                SELECT_PICTURE_LONG_NEWS -> FirebaseManager.uploadLongNewsToFirebase(
                    title,
                    date,
                    ConvertStringToHtml.txtToHtml(content),
                    link, it - 1)
            }

            lastIdNewsLD.postValue(it - 1)
            return
        }
        Toast.makeText(this, "lastIdNews $lastIdNewsLD", Toast.LENGTH_LONG).show()
    }
}
