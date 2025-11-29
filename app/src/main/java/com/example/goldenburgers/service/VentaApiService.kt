package com.example.goldenburgers.service

import retrofit2.http.*
import com.example.goldenburgers.model.dto.VentaDTO
import com.example.goldenburgers.model.dto.BoletaDTO
import com.example.goldenburgers.model.dto.DevolucionDTO

//Ventas asociadas a boletas

interface VentaApiService {
    @GET("api/ventas")
    suspend fun getVentas(): List<VentaDTO>

    @POST("api/ventas")
    suspend fun crearVenta(@Body venta: VentaDTO): VentaDTO

    @GET("api/ventas/{id}")
    suspend fun getVenta(@Path("id") id: Long): VentaDTO


    @GET("api/boletas")
    suspend fun getBoletas(): List<BoletaDTO>

    @POST("api/boletas")
    suspend fun crearBoleta(@Body boleta: BoletaDTO): BoletaDTO

    @GET("api/boletas/{id}")
    suspend fun getBoleta(@Path("id") id: Long): BoletaDTO

    @POST("api/devoluciones")
    suspend fun crearDevolucion(@Body devolucion: DevolucionDTO): DevolucionDTO

    @GET("api/devoluciones")
    suspend fun getDevoluciones(): List<DevolucionDTO>

    @GET("api/devoluciones/{id}")
    suspend fun getDevolucion(@Path("id") id: Long): DevolucionDTO
}
