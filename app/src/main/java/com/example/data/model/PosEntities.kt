package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val username: String,
    val password: String,
    val role: String // "pemilik" or "kasir"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emoji: String = "📦",
    val nama: String,
    val kategori: String,
    val modal: Double = 0.0,
    val jual: Double = 0.0,
    val grosirMin: Int = 0,
    val grosirHarga: Double = 0.0,
    val varianJson: String = "[]", // List of String
    val toppingJson: String = "[]", // List of ToppingItem
    val stok: Int = 0,
    val resepJson: String = "[]", // List of RecipeIngredient
    val aktif: Boolean = true
)

@Entity(tableName = "raw_materials")
data class RawMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val harga: Double,
    val isi: Double,
    val stok: Double,
    val satuan: String = "gram"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = false) val id: String, // e.g. "TRX-123456"
    val shiftId: String = "", // e.g. "SHIFT-20260816-001"
    val waktu: Long = System.currentTimeMillis(),
    val tanggalISO: String, // "YYYY-MM-DD"
    val tanggalStr: String,
    val kasir: String,
    val pelanggan: String = "Umum",
    val tipePesanan: String = "Dine-In",
    val nomorMeja: String = "-",
    val metode: String = "tunai", // "tunai", "qris", "transfer"
    val subtotal: Double = 0.0,
    val diskon: Double = 0.0,
    val pajak: Double = 0.0,
    val totalPemasukan: Double = 0.0,
    val totalModal: Double = 0.0,
    val uangBayar: Double = 0.0,
    val uangKembali: Double = 0.0,
    val itemsJson: String = "[]" // List of CartItem
)

@Entity(tableName = "hold_orders")
data class HoldOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val waktuStr: String,
    val pelanggan: String,
    val nomorMeja: String,
    val tipePesanan: String,
    val itemsJson: String
)

@Entity(tableName = "kasbon")
data class KasbonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trxId: String,
    val tanggalISO: String,
    val tanggalStr: String,
    val pelanggan: String,
    val tipePesanan: String,
    val nomorMeja: String,
    val total: Double,
    val status: String = "Belum Lunas", // "Belum Lunas", "Lunas"
    val itemsJson: String
)

@Entity(tableName = "cash_expenses")
data class CashExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: String = "",
    val tanggalISO: String,
    val tanggalStr: String,
    val kasir: String,
    val keterangan: String,
    val nominal: Double
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftCode: String, // e.g. "SHIFT-20260816-1102"
    val kasirUsername: String,
    val kasirNama: String,
    val waktuBuka: Long = System.currentTimeMillis(),
    val waktuBukaStr: String, // "16/08/2026 08:00"
    val waktuTutup: Long? = null,
    val waktuTutupStr: String? = null, // "16/08/2026 15:30"
    val modalAwal: Double = 0.0,
    val omsetTunai: Double = 0.0,
    val omsetDigital: Double = 0.0,
    val totalPengeluaran: Double = 0.0,
    val totalTransaksi: Int = 0,
    val uangFisikAktual: Double = 0.0,
    val selisihKas: Double = 0.0,
    val catatan: String = "",
    val status: String = "OPEN" // "OPEN" or "CLOSED"
)

@Entity(tableName = "hpp_history")
data class HppHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val waktuStr: String,
    val namaProduk: String,
    val hppFinal: Double,
    val saranTargetFC: Double,
    val m35: Double,
    val m50: Double,
    val jumlahUnitFinal: Int,
    val totalBahan: Double,
    val totalBiayaLain: Double,
    val targetFCPersen: Double,
    val bahanListJson: String = "[]",
    val statusMargin: String = "✅ HPP Profesional V6.7"
)

// Data Transfer / Helper Classes for JSON serialization or state
data class ToppingItem(
    val nama: String,
    val harga: Double
)

data class RecipeIngredient(
    val idBahan: Long? = null,
    val nama: String,
    val pakai: Double
)

data class CartItem(
    val cartKey: String,
    val id: Long,
    val nama: String,
    val modal: Double,
    val jualDasar: Double,
    val grosirMin: Int,
    val grosirHarga: Double,
    val toppingPrice: Double,
    val qty: Int,
    val subtotal: Double
)

data class StoreSettings(
    val namaToko: String = "Toko POS Kuliner",
    val alamatToko: String = "Jl. Raya Utama No. 123, Jakarta",
    val noTelpToko: String = "0812-3456-7890",
    val pesanStruk: String = "Terima kasih atas kunjungan Anda!",
    val qrisUrl: String = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=TOKO_KASIGRATIS_DEFAULT",
    val bankNama: String = "BCA",
    val bankNoRek: String = "1234567890",
    val bankPemilik: String = "PT KasiGRatis Indonesia",
    val modalAwalLaci: Double = 0.0
)

