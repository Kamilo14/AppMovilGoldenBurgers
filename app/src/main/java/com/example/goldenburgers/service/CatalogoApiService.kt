package com.example.goldenburgers.service

import retrofit2.http.*
import okhttp3.MultipartBody
import com.example.goldenburgers.model.dto.ProductoDTO
import com.example.goldenburgers.model.dto.CategoriaDTO

interface CatalogoApiService {
    @GET("api/catalogo/productos")
    suspend fun getProductos(): List<ProductoDTO>

    @GET("api/catalogo/productos/{id}")
    suspend fun getProducto(@Path("id") id: Long): ProductoDTO

    @GET("api/catalogo/productos/categoria/{idCategoria}")
    suspend fun getProductosPorCategoria(@Path("idCategoria") idCategoria: Long): List<ProductoDTO>


    @GET("api/catalogo/categorias")
    suspend fun getCategorias(): List<CategoriaDTO>


}
