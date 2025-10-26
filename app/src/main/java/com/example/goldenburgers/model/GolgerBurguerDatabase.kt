package com.example.goldenburgers.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.vo.Database
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Esta es la clase principal de la base de datos Room.
 * Sirve como el punto de acceso central a todos los datos persistentes de la aplicación.
 */

@Database(entities = [Producto::class, User::class], version = 3, exportSchema = false)
abstract class GolgerBurguerDatabase : RoomDatabase() {

    // Por cada tabla, se declara una función abstracta que devuelve su DAO (Data Access Object).
    // Room se encargará de generar la implementación de estas funciones.
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao

    // Se usa un `companion object` para implementar el patrón Singleton.

    companion object {
        // La anotación @Volatile asegura que el valor de INSTANCE esté siempre actualizado
        // y sea visible para todos los hilos de ejecución de la app.
        @Volatile
        private var INSTANCE: GolgerBurguerDatabase? = null

        /**
         * Esta es la función pública que se usará en toda la app para obtener la instancia
         * única de la base de datos.
         */
        fun getDatabase(context: Context): GolgerBurguerDatabase {
            // Si la instancia ya existe, la devuelve directamente para no crear una nueva.
            // Si es nula, entra en el bloque `synchronized` para crearla de forma segura.
            return INSTANCE ?: synchronized(this) {
                // El bloque `synchronized` previene que dos hilos intenten crear la
                // base de datos al mismo tiempo, lo que podría causar errores (race condition).
                val instance = Room.databaseBuilder(
                    context.applicationContext, // El contexto de la aplicación.
                    GolgerBurguerDatabase::class.java, // La clase de base de datos.
                    "golgerburguer_database" // El nombre del archivo de la base de datos en el dispositivo.
                )
                    // Si incrementa la versión de la BD,
                    // Room borrará la base de datos antigua y la creará de nuevo.
                    .fallbackToDestructiveMigration()
                    // Aquí se añade un "callback", que es una función que se ejecuta en ciertos
                    // momentos clave del ciclo de vida de la base de datos.
                    .addCallback(object : Callback() {
                        /**
                         * La función `onCreate` se ejecuta UNA SOLA VEZ, la primera vez que la
                         * base de datos se crea en el dispositivo. Es el lugar perfecto para
                         * insertar datos iniciales, ejmplo: catálogo de productos.
                         */
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Lanza una corutina en un hilo secundario (IO) para no bloquear
                            // el hilo principal mientras se insertan los datos.
                            CoroutineScope(Dispatchers.IO).launch {
                                getDatabase(context).productDao().insertAll(FakeProductDataSource.products)
                            }
                        }
                    })
                    .build() // Finalmente, se construye la instancia de la base de datos.

                // Guarda la nueva instancia en la variable estática y la devuelve.
                INSTANCE = instance
                instance
            }
        }
    }
}
