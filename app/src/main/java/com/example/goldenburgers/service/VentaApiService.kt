package com.example.goldenburgers.service

import retrofit2.http.*
import com.example.goldenburgers.model.dto.VentaDTO
import com.example.goldenburgers.model.dto.BoletaDTO
import com.example.goldenburgers.model.dto.DevolucionDTO

//Ventas asociadas a boletas

interface VentaApiService {
    // Ventas
    @GET("ventas")
    suspend fun getVentas(): List<VentaDTO>

    @POST("ventas")
    suspend fun crearVenta(@Body venta: VentaDTO): VentaDTO

    @GET("ventas/{id}")
    suspend fun getVenta(@Path("id") id: Long): VentaDTO

    @PUT("ventas/{id}")
    suspend fun actualizarVenta(@Path("id") id: Long, @Body venta: VentaDTO): VentaDTO

    @DELETE("ventas/{id}")
    suspend fun eliminarVenta(@Path("id") id: Long)

    // Boletas
    @GET("boletas")
    suspend fun getBoletas(): List<BoletaDTO>

    @POST("boletas")
    suspend fun crearBoleta(@Body boleta: BoletaDTO): BoletaDTO

    @GET("boletas/{id}")
    suspend fun getBoleta(@Path("id") id: Long): BoletaDTO

    @PUT("boletas/{id}")
    suspend fun actualizarBoleta(@Path("id") id: Long, @Body boleta: BoletaDTO): BoletaDTO

    @DELETE("boletas/{id}")
    suspend fun eliminarBoleta(@Path("id") id: Long)

    // Devoluciones
    @POST("devoluciones")
    suspend fun crearDevolucion(@Body devolucion: DevolucionDTO): DevolucionDTO

    @GET("devoluciones")
    suspend fun getDevoluciones(): List<DevolucionDTO>

    @GET("devoluciones/{id}")
    suspend fun getDevolucion(@Path("id") id: Long): DevolucionDTO
}
