package com.bagi.soreknetmanager.network

import com.bagi.soreknetmanager.model.ImgurResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ServiceApi {
    @Multipart
    @POST("image")
    suspend fun postImage(@Part fileRequestBody: MultipartBody.Part): ImgurResponse
}