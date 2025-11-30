package com.example.goldenburgers.service

import retrofit2.http.*
import okhttp3.MultipartBody
import com.example.goldenburgers.model.dto.ProductoDTO
import com.example.goldenburgers.model.dto.CategoriaDTO

interface CatalogoApiService {

    //Obtiene los productos disponibles en el catálogo
    @GET("catalogo/productos")
    suspend fun getProductos(): List<ProductoDTO>

    @GET("catalogo/productos/{id}")
    suspend fun getProducto(@Path("id") id: Long): ProductoDTO

    @GET("catalogo/productos/categoria/{idCategoria}")
    suspend fun getProductosPorCategoria(@Path("idCategoria") idCategoria: Long): List<ProductoDTO>


    @GET("catalogo/categorias")
    suspend fun getCategorias(): List<CategoriaDTO>

}
