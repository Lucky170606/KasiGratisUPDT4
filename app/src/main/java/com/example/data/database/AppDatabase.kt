package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.PosDao
import com.example.data.model.CashExpenseEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.HoldOrderEntity
import com.example.data.model.HppHistoryEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.ShiftEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        RawMaterialEntity::class,
        TransactionEntity::class,
        HoldOrderEntity::class,
        KasbonEntity::class,
        CashExpenseEntity::class,
        HppHistoryEntity::class,
        ShiftEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val populateMutex = Mutex()

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasigratis_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default data in background
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.let { database ->
                                    populateDatabase(database.posDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(dao: PosDao) {
            populateMutex.withLock {
                try {
                    // 1. Deduplicate any existing duplicate entries (from prior duplicate seeds)
                    val existingUsers = dao.getAllUsersDirect()
                    val seenUsernames = mutableSetOf<String>()
                    for (u in existingUsers) {
                        val key = u.username.lowercase().trim()
                        if (seenUsernames.contains(key)) {
                            dao.deleteUser(u.id)
                        } else {
                            seenUsernames.add(key)
                        }
                    }

                    val existingCategories = dao.getAllCategoriesDirect()
                    val seenCategories = mutableSetOf<String>()
                    for (c in existingCategories) {
                        val key = c.name.lowercase().trim()
                        if (seenCategories.contains(key)) {
                            dao.deleteCategory(c.id)
                        } else {
                            seenCategories.add(key)
                        }
                    }

                    val existingRawMaterials = dao.getAllRawMaterialsDirect()
                    val seenRawMaterials = mutableSetOf<String>()
                    for (rm in existingRawMaterials) {
                        val key = rm.nama.lowercase().trim()
                        if (seenRawMaterials.contains(key)) {
                            dao.deleteRawMaterial(rm.id)
                        } else {
                            seenRawMaterials.add(key)
                        }
                    }

                    val existingProducts = dao.getAllProductsDirect()
                    val seenProducts = mutableSetOf<String>()
                    for (p in existingProducts) {
                        val key = p.nama.lowercase().trim()
                        if (seenProducts.contains(key)) {
                            dao.deleteProduct(p.id)
                        } else {
                            seenProducts.add(key)
                        }
                    }

                    // 2. Insert Default Users if table is empty
                    if (dao.getAllUsersDirect().isEmpty()) {
                        dao.insertUser(UserEntity(nama = "Pemilik Toko", username = "owner", password = "123", role = "pemilik"))
                        dao.insertUser(UserEntity(nama = "Kasir Utama", username = "kasir", password = "123", role = "kasir"))
                    }

                    // 3. Insert Default Categories if table is empty
                    if (dao.getAllCategoriesDirect().isEmpty()) {
                        dao.insertCategory(CategoryEntity(name = "Makanan"))
                        dao.insertCategory(CategoryEntity(name = "Minuman"))
                        dao.insertCategory(CategoryEntity(name = "Snack"))
                    }

                    // 4. Insert Default Raw Materials if table is empty
                    if (dao.getAllRawMaterialsDirect().isEmpty()) {
                        dao.insertRawMaterial(RawMaterialEntity(nama = "Biji Kopi Arabika", harga = 120000.0, isi = 1000.0, stok = 5000.0, satuan = "gram"))
                        dao.insertRawMaterial(RawMaterialEntity(nama = "Susu Fresh Milk", harga = 18000.0, isi = 1000.0, stok = 10000.0, satuan = "ml"))
                        dao.insertRawMaterial(RawMaterialEntity(nama = "Gula Pasir", harga = 15000.0, isi = 1000.0, stok = 3000.0, satuan = "gram"))
                    }

                    // 5. Insert Default Products if table is empty
                    if (dao.getAllProductsDirect().isEmpty()) {
                        val kopiVarian = "[\"Ice\", \"Hot\"]"
                        val kopiTopping = "[{\"nama\":\"Extra Shot\",\"harga\":5000.0},{\"nama\":\"Boba\",\"harga\":3000.0}]"
                        val kopiResep = "[{\"idBahan\":1,\"nama\":\"Biji Kopi Arabika\",\"pakai\":18.0},{\"idBahan\":2,\"nama\":\"Susu Fresh Milk\",\"pakai\":150.0}]"

                        val rotiVarian = "[\"Cokelat\", \"Keju\"]"
                        val rotiTopping = "[{\"nama\":\"Extra Keju\",\"harga\":3000.0}]"

                        dao.insertProduct(
                            ProductEntity(
                                emoji = "☕",
                                nama = "Kopi Susu",
                                kategori = "Minuman",
                                modal = 5000.0,
                                jual = 12000.0,
                                grosirMin = 5,
                                grosirHarga = 10000.0,
                                varianJson = kopiVarian,
                                toppingJson = kopiTopping,
                                stok = 50,
                                resepJson = kopiResep,
                                aktif = true
                            )
                        )

                        dao.insertProduct(
                            ProductEntity(
                                emoji = "🍞",
                                nama = "Roti Bakar",
                                kategori = "Makanan",
                                modal = 4000.0,
                                jual = 10000.0,
                                grosirMin = 0,
                                grosirHarga = 0.0,
                                varianJson = rotiVarian,
                                toppingJson = rotiTopping,
                                stok = 30,
                                resepJson = "[]",
                                aktif = true
                            )
                        )

                        dao.insertProduct(
                            ProductEntity(
                                emoji = "🍟",
                                nama = "Keripik Singkong",
                                kategori = "Snack",
                                modal = 3000.0,
                                jual = 7000.0,
                                grosirMin = 10,
                                grosirHarga = 6000.0,
                                varianJson = "[]",
                                toppingJson = "[]",
                                stok = 15,
                                resepJson = "[]",
                                aktif = true
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore if error occurs during population
                }
            }
        }
    }
}
