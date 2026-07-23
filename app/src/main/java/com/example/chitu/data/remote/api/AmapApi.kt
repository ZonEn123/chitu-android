package com.example.chitu.data.remote.api

import com.example.chitu.data.remote.dto.AmapPoiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AmapApi {

    @GET("/v3/place/around")
    suspend fun searchAround(
        @Query("key") key: String,
        @Query("location") location: String,
        @Query("radius") radius: Int = 50000,
        @Query("types") types: String = "服务区",
        @Query("offset") offset: Int = 20,
        @Query("page") page: Int = 1,
        @Query("extensions") extensions: String = "all"
    ): AmapPoiResponse
}
