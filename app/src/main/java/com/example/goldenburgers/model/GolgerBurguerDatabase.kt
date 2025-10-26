package com.example.goldenburgers.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Esta es la clase principal de mi base de datos Room.
 * Sirve como el punto de acceso central a todos los datos persistentes de la aplicación.
 */
// La anotación @Database es clave. Aquí le digo a Room qué tablas (entities) va a tener
// mi base de datos y qué versión del esquema estamos usando. Si cambio la estructura de
// una tabla, debo incrementar este número de versión.
@Database(entities = [Producto::class, User::class], version = 3, exportSchema = false)
abstract class GolgerBurguerDatabase : RoomDatabase() {

    // Por cada tabla, declaro una función abstracta que devuelve su DAO (Data Access Object).
    // Room se encargará de generar la implementación de estas funciones por mí.
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao

    // Uso un `companion object` para implementar el patrón Singleton.
    // Esto asegura que solo exista UNA instancia de la base de datos en toda la aplicación,
    // lo que es muy eficiente y evita problemas de concurrencia.
    companion object {
        // La anotación @Volatile asegura que el valor de INSTANCE esté siempre actualizado
        // y sea visible para todos los hilos de ejecución de la app.
        @Volatile
        private var INSTANCE: GolgerBurguerDatabase? = null

        /**
         * Esta es la función pública que usaré en toda mi app para obtener la instancia
         * única de la base de datos.
         */
        fun getDatabase(context: Context): GolgerBurguerDatabase {
            // Si la instancia ya existe, la devuelvo directamente para no crear una nueva.
            // Si es nula, entro en el bloque `synchronized` para crearla de forma segura.
            return INSTANCE ?: synchronized(this) {
                // El bloque `synchronized` previene que dos hilos intenten crear la
                // base de datos al mismo tiempo, lo que podría causar errores (race condition).
                val instance = Room.databaseBuilder(
                    context.applicationContext, // El contexto de la aplicación.
                    GolgerBurguerDatabase::class.java, // Mi clase de base de datos.
                    "golgerburguer_database" // El nombre del archivo de la base de datos en el dispositivo.
                )
                    // Esta es mi estrategia de migración. Si incremento la versión de la BD,
                    // Room borrará la base de datos antigua y la creará de nuevo.
                    // Es muy útil durante el desarrollo, pero para producción usaría migraciones reales.
                    .fallbackToDestructiveMigration()
                    // Aquí añado un "callback", que es una función que se ejecuta en ciertos
                    // momentos clave del ciclo de vida de la base de datos.
                    .addCallback(object : Callback() {
                        /**
                         * La función `onCreate` se ejecuta UNA SOLA VEZ, la primera vez que la
                         * base de datos se crea en el dispositivo. Es el lugar perfecto para
                         * insertar datos iniciales, como mi catálogo de productos.
                         */
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Lanzo una corutina en un hilo secundario (IO) para no bloquear
                            // el hilo principal mientras inserto los datos.
                            CoroutineScope(Dispatchers.IO).launch {
                                getDatabase(context).productDao().insertAll(FakeProductDataSource.products)
                            }
                        }
                    })
                    .build() // Finalmente, construyo la instancia de la base de datos.

                // Guardo la nueva instancia en mi variable estática y la devuelvo.
                INSTANCE = instance
                instance
            }
        }
    }
}
