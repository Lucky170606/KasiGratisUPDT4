package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility helper untuk standardisasi zona waktu Waktu Indonesia Barat (WIB / Asia/Jakarta / GMT+7)
 * di seluruh modul aplikasi (Kasir, Transaksi, Shift, Laporan, Kasbon, Backup, PDF Export).
 */
object DateTimeHelper {
    val TIMEZONE_WIB: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val LOCALE_ID: Locale = Locale("id", "ID")

    /**
     * Membuat SimpleDateFormat yang sudah dipasangi TimeZone WIB (Asia/Jakarta)
     */
    fun createFormatter(pattern: String, locale: Locale = LOCALE_ID): SimpleDateFormat {
        return SimpleDateFormat(pattern, locale).apply {
            timeZone = TIMEZONE_WIB
        }
    }

    /**
     * Mendapatkan tanggal & jam format "dd/MM/yyyy HH:mm 'WIB'"
     */
    fun formatDateTimeWib(date: Date = Date()): String {
        return createFormatter("dd/MM/yyyy HH:mm").format(date) + " WIB"
    }

    /**
     * Mendapatkan tanggal format ISO YYYY-MM-DD sesuai tanggal di WIB
     */
    fun formatIsoDateWib(date: Date = Date()): String {
        return createFormatter("yyyy-MM-dd", Locale.US).format(date)
    }

    /**
     * Mendapatkan tanggal format display "dd/MM/yyyy"
     */
    fun formatDateDisplayWib(date: Date = Date()): String {
        return createFormatter("dd/MM/yyyy").format(date)
    }

    /**
     * Mendapatkan jam menit format "HH:mm 'WIB'"
     */
    fun formatTimeWib(date: Date = Date()): String {
        return createFormatter("HH:mm").format(date) + " WIB"
    }

    /**
     * Mendapatkan tanggal panjang "dd MMMM yyyy HH:mm 'WIB'"
     */
    fun formatFullDateTimeWib(date: Date = Date()): String {
        return createFormatter("dd MMMM yyyy HH:mm").format(date) + " WIB"
    }

    /**
     * Format timestamp untuk nama file export/backup "yyyyMMdd_HHmmss"
     */
    fun formatTimestampForFile(date: Date = Date()): String {
        return createFormatter("yyyyMMdd_HHmmss", Locale.US).format(date)
    }

    /**
     * Format kode shift "SHIFT-yyyyMMdd-HHmmss"
     */
    fun formatShiftCode(date: Date = Date()): String {
        return "SHIFT-${createFormatter("yyyyMMdd-HHmmss", Locale.US).format(date)}"
    }

    /**
     * Membersihkan duplikasi string "WIB" (seperti WIBWIB) agar tampil rapi.
     */
    fun cleanWib(str: String?): String {
        if (str.isNullOrBlank()) return "-"
        var s = str.trim()
        while (s.contains("WIBWIB") || s.contains("WIB WIB") || s.contains("WIB  WIB")) {
            s = s.replace("WIBWIB", "WIB").replace("WIB WIB", "WIB").replace("WIB  WIB", "WIB").trim()
        }
        return s
    }
}
