package com.example.goldenburgers.model
import com.example.goldenburgers.R
object FakeProductDataSource {


    val products = listOf(
        Producto(
            id = 1,
            nombre = "Hamburguesa Clásica",
            descripcion = "Hamburguesa 120g, doble chedar, pepinillos, salsa Golden, tomate, lechuga, cebolla morada y pepinillos.",
            precio = 6990.0,
            imagenReferencia = R.drawable.clasica, // Referencia local
            categoria = "Hamburguesa"
        ),
        Producto(
            id = 2,
            nombre = "Hamburguesa Champiñon",
            descripcion = "Hamburguesa 120g, queso mantecoso, champiñones, cebolla caramelizada y Mayonesa.",
            precio = 8790.0,
            imagenReferencia = R.drawable.champinon, // Referencia local
            categoria = "Hamburguesa",
            esFavorito = true // Ejemplo de favorito inicial
        ),
        Producto(
            id = 3,
            nombre = "Hamburguesa Golden",
            descripcion = "Hamburguesa 120g, doble cheddar, pepinillos, tocino, salsa golden.",
            precio = 7990.0,
            imagenReferencia = R.drawable.golden, // Referencia local
            categoria = "Hamburguesa"
        ),
        Producto(
            id = 4,
            nombre = "Hamburguesa Italiana",
            descripcion = "Hamburguesa 120g, Palta, tomate y mayonesa.",
            precio = 2000.0, // Nota: Este precio parece bajo para una hamburguesa, revísalo si es necesario
            imagenReferencia = R.drawable.italiana, // Referencia local
            categoria = "Hamburguesa"
        ),
        Producto(
            id = 5,
            nombre = "Papas medianas",
            descripcion = "Papas cortadas en bastones finos.",
            precio = 2000.0,
            imagenReferencia = R.drawable.papasfritas, // Referencia local
            categoria = "Frito",
            esFavorito = true
        ),
        Producto(
            id = 6,
            nombre = "Papas Golden",
            descripcion = "Papas de la casa con topping de tocino",
            precio = 2500.0,
            imagenReferencia = R.drawable.papasgolden, // Referencia local
            categoria = "Frito",
            esFavorito = true
        ),
        Producto(
            id = 7,
            nombre = "Chicken de pops",
            descripcion = "Bolitas de pollos",
            precio = 3000.0,
            imagenReferencia = R.drawable.chickenpop, // Referencia local
            categoria = "Frito",
            esFavorito = true
        ),
        Producto(
            id = 8,
            nombre = "Jalapeño Frito",
            descripcion = "Bolitas Fritas con jalapeños en su interior",
            precio = 3000.0,
            imagenReferencia = R.drawable.jalapenos, // Referencia local
            categoria = "Frito",
            esFavorito = true
        ),
        Producto(
            id = 9,
            nombre = "Coca Cola",
            descripcion = "Lata de Bebida fria.",
            precio = 1500.0,
            imagenReferencia = R.drawable.cocacola, // Referencia local
            categoria = "Bebida",
            esFavorito = true
        ),
        Producto(
            id = 10,
            nombre = "Sprite",
            descripcion = "Lata de Bebida fria.",
            precio = 1500.0,
            imagenReferencia = R.drawable.sprite, // Referencia local
            categoria = "Bebida",
            esFavorito = true
        ),
        Producto(
            id = 11,
            nombre = "Fanta",
            descripcion = "Lata de Bebida fria.",
            precio = 1500.0,
            imagenReferencia = R.drawable.fanta, // Referencia local
            categoria = "Bebida",
            esFavorito = true
        ),
        Producto(
            id = 12,
            nombre = "Jugo Jumex",
            descripcion = "Lata de Jugo frio.",
            precio = 1500.0,
            imagenReferencia = R.drawable.jumex, // Referencia local
            categoria = "Bebida",
            esFavorito = true
        ),
    )
}


