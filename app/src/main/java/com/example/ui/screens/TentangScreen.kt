package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SuccessEmerald
import com.example.util.PdfReportHelper

@Composable
fun TentangScreen() {
    val context = LocalContext.current
    var showReleaseHistory by remember { mutableStateOf(false) }

    val openInstagram: () -> Unit = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/ekailaika/"))
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/ekailaika"))
            context.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryIndigo, SecondaryViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Logo KasiGratis",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "KasiGR-atis POS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryIndigo
                    )
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "🚀 Versi v6.7.7 (Peningkatan Keamanan & UI Polished)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Mempermudah Operasional & Pembukuan Bisnis Anda",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card Khusus Developer Opsi B (Clickable ke Instagram)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryIndigo.copy(alpha = 0.07f)
                    ),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openInstagram() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Karya & Dedikasi dari:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📸", fontSize = 14.sp)
                            Text(
                                text = "Eka Ilaika (@ekailaika)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryViolet,
                                textDecoration = TextDecoration.Underline
                            )
                            Text("↗", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SecondaryViolet)
                        }

                        Text(
                            text = "Sebuah kontribusi nyata untuk mendukung pertumbuhan wirausaha lokal di seluruh Indonesia.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "🇮🇩 Dukungan Penuh #UMKMBangkit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Catatan Rilis / Patch Notes (Model Accordion Bersih & Profesional)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showReleaseHistory = !showReleaseHistory }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📜", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Riwayat Pembaruan & Patch Notes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (showReleaseHistory) "Ketuk untuk melipat catatan" else "Ketuk untuk melihat riwayat versi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigo.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = if (showReleaseHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showReleaseHistory) "Tutup" else "Buka",
                            tint = PrimaryIndigo,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showReleaseHistory) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Versi Terbaru (v6.7.7)
                        ReleaseVersionItem(
                            version = "v6.7.7 (Keamanan Impor Cadangan & Penyempurnaan UI)",
                            date = "20 Agustus 2026",
                            isLatest = true,
                            notes = listOf(
                                "🔒 Restriksi Impor Cadangan Toko (*Fresh Install Only*): Tombol impor file cadangan database di layar login/landing kini hanya muncul saat instalasi baru (database kosong tanpa transaksi/shift), mencegah akses iseng atau penimpaan database oleh pihak tidak berwenang.",
                                "🎨 Desain Kartu Produk Datar & Bersih (*Flat Clean Design*): Menghilangkan seluruh bayangan elevation berlebih pada kartu produk di halaman Kasir sehingga border 1.dp tipis tampil sangat rapi dan presisi tanpa efek gradasi tebal."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Versi Sebelumnya (v6.7.6)
                        ReleaseVersionItem(
                            version = "v6.7.6 (Perbaikan Bug Kritikal & Stabilitas Sistem)",
                            date = "19 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "🔒 Perbaikan Bug Autentikasi & Akun Default: Mencegah pemulihan otomatis (resurreksi) akun default 'owner' & 'kasir' yang telah dihapus saat pengguna mencoba masuk dengan username/password lama.",
                                "🔑 Penyelarasan Hak Akses Pemilik: Pengecekan role 'pemilik' & 'owner' diseragamkan di seluruh modul (shift, laporan, & panduan operasional).",
                                "📒 Rekapitulasi Kasbon & Riwayat Transaksi: Transaksi pembayaran kasbon tersimpan penuh di riwayat transaksi utama & laporan omset kasbon.",
                                "📊 Akurasi Modal HPP Pelunasan: Pelunasan piutang/kasbon diset HPP 0.0 agar arus kas masuk tidak menggandakan porsi modal HPP.",
                                "🛡️ Anti-Crash Currency & Safe JSON: Pengamanan nilai NaN/Infinity pada format Rupiah & parsing opsional pada pemulihan pesanan tertahan.",
                                "🗑️ Proteksi Akun & Keranjang: Mencegah penghapusan akun milik sendiri yang sedang aktif login & otomatis membersihkan keranjang saat menu dihapus.",
                                "📄 Standar Margin & Header PDF ISO A4: Penyesuaian aturan cetak @page (margin 12mm x 15mm), tata letak header simetris, dan proteksi page-break pada seluruh laporan PDF (Finansial, Shift, HPP, & Kasbon)."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Versi Sebelumnya (v6.7.5)
                        ReleaseVersionItem(
                            version = "v6.7.5 (Sistem Shift Kasir & Dokumen PDF)",
                            date = "16 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "⏱️ Manajemen Shift Kasir Terintegrasi: Buka & Tutup Shift, input modal awal laci kasir, pencatatan kas keluar operasional shift, dan deteksi selisih kas fisik riil.",
                                "📄 Dokumen Ekspor & Cetak PDF Resmi: Cetak dan simpan Laporan Shift Kasir, Laporan Penjualan Finansial A4, Lembar Resep HPP BOM, Daftar Kasbon Piutang, Struk Transaksi, dan Patch Notes dalam format PDF.",
                                "🗄️ Sinkronisasi Backup Database SQLite: Relasi shift kasir dan seluruh tabel (Toko, Menu, BOM, Transaksi, Kasbon, Shift) terbackup & pulih 100% aman dalam format JSON.",
                                "📖 Pembaruan Buku Panduan: Panduan Pemilik (Owner) dan Kasir telah disesuaikan dengan alur cetak dokumen PDF dan operasional serah terima shift."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Versi Sebelumnya (v6.7.4)
                        ReleaseVersionItem(
                            version = "v6.7.4 (Penyelarasan UI & Dark Mode)",
                            date = "15 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "📐 Penyelarasan Modal Dialog: Seluruh judul modal dialog & sub-dialog konfirmasi (seperti \"Konfirmasi Akhir\") simetris di tengah.",
                                "🌙 Kontras Dark Mode Adaptif: Teks deskripsi dan tombol pada modal konfirmasi otomatis terang dan kontras saat Mode Gelap diaktifkan.",
                                "⌨️ Auto-Dismiss Keyboard: Menekan 'Done' pada kolom pencarian otomatis menurunkan keyboard.",
                                "📅 Presisi Laporan WIB: Filter tanggal kustom presisi ke 00:00:00 - 23:59:59 WIB."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Versi Sebelumnya (v6.7.3)
                        ReleaseVersionItem(
                            version = "v6.7.3 (Pencadangan JSON)",
                            date = "14 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "💾 Modul Cadangkan & Pulihkan (Backup & Restore) SQLite JSON Lengkap pada menu Pengaturan.",
                                "📤 Ekspor & Bagikan berkas .json (seluruh produk, resep BOM, kasbon, transaksi, profil toko) ke File Manager/WA/Drive.",
                                "📥 Dua opsi pemulihan data instan: 'Timpa & Gantikan Semua' atau 'Gabungkan Data (Merge)'.",
                                "📋 Fitur Salin & Tempel teks JSON cepat untuk migrasi data antar perangkat tanpa internet."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Versi Sebelumnya (v6.7.2)
                        ReleaseVersionItem(
                            version = "v6.7.2 (Peningkatan Antarmuka)",
                            date = "14 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "✨ Pilihan Kategori Produk otomatis & interaktif pada formulir tambah menu.",
                                "🏷️ Badge Kategori visual pada kartu daftar produk untuk navigasi cepat.",
                                "🎨 Penataan layout label Stok agar rapi dan tidak melipat vertikal pada layar kompak.",
                                "💎 Penyegaran halaman Tentang dengan identitas karya yang lebih elegan & profesional."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Versi Sebelumnya (v6.7.1)
                        ReleaseVersionItem(
                            version = "v6.7.1 (Peningkatan Fitur)",
                            date = "13 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "🧮 Modul Kalkulator & Manajemen Resep HPP Bahan Baku otomatis.",
                                "🍨 Dukungan Varian Topping & Add-ons dengan kustomisasi harga per item.",
                                "📱 Optimalisasi QRIS Dinamis dan Cetak Ulang Struk Kasir.",
                                "📊 Filter Laporan Penjualan Harian, Mingguan, dan Bulanan yang lebih responsif."
                            )
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Rilis Perdana (v6.7.0)
                        ReleaseVersionItem(
                            version = "v6.7.0 (Rilis Perdana Resmi)",
                            date = "12 Agustus 2026",
                            isLatest = false,
                            notes = listOf(
                                "🧾 Transaksi Kasir POS Cepat dengan Cetak Struk Printer Thermal Bluetooth.",
                                "💳 Dukungan Multimetode Bayar (Tunai, QRIS, Transfer, Kasbon & Kembalian).",
                                "📈 Laporan Keuangan HPP & Laba Bersih Akurat + Pagination 15 Data.",
                                "📝 Pencatatan Kasbon & Aging Piutang Pelanggan Terintegrasi.",
                                "🥣 Kalkulasi Modal Bahan Baku (Resep HPP) & Peringatan Margin Tipis.",
                                "📑 Ekspor Laporan PDF Lengkap (Shift, Finansial, Kasbon, & Resep HPP).",
                                "🔒 Hak Akses Multirole (Mode Kasir & Pemilik Toko ber-PIN)."
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                PdfReportHelper.generateAndPrintReleaseNotesPdf(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🖨️ Cetak / Unduh Release Notes (PDF)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseVersionItem(
    version: String,
    date: String,
    isLatest: Boolean,
    notes: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = version,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLatest) PrimaryIndigo else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isLatest) SuccessEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = date,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLatest) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        notes.forEach { note ->
            Text(
                text = note,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )
        }
    }
}

