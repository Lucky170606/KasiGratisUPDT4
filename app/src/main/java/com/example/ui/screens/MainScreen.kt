package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.KasiGratisTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.PosViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PosViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showLoginScreen by viewModel.showLoginScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val showGuideRole by viewModel.showGuideModal.collectAsState()

    KasiGratisTheme(darkTheme = isDarkMode) {
        if (showLoginScreen || currentUser == null) {
            LoginScreen(
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode(it) }
            )
        } else {
            Scaffold(
                topBar = {
                    Surface(
                        shadowElevation = 4.dp,
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        "🛒 KasiGR-atis POS",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        "👤 ${currentUser?.nama ?: ""} (${currentUser?.role?.uppercase()})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.toggleDarkMode(!isDarkMode) }) {
                                    Text(if (isDarkMode) "🌙" else "☀️", fontSize = 16.sp)
                                }

                                IconButton(onClick = { viewModel.logout() }) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = DangerRed)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                bottomBar = {
                    Surface(
                        shadowElevation = 8.dp,
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                        val role = currentUser?.role ?: "kasir"

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Kasir,
                            onClick = { viewModel.selectTab(NavTab.Kasir) },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                            label = { Text("Kasir", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )

                        if (role == "pemilik") {
                            NavigationBarItem(
                                selected = selectedTab == NavTab.Produk,
                                onClick = { viewModel.selectTab(NavTab.Produk) },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                label = { Text("Produk", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                            )
                        }

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Laporan,
                            onClick = { viewModel.selectTab(NavTab.Laporan) },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                            label = { Text("Laporan", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Kasbon,
                            onClick = { viewModel.selectTab(NavTab.Kasbon) },
                            icon = { Icon(Icons.Default.Book, contentDescription = null) },
                            label = { Text("Kasbon", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )

                        if (role == "pemilik") {
                            NavigationBarItem(
                                selected = selectedTab == NavTab.Pengaturan,
                                onClick = { viewModel.selectTab(NavTab.Pengaturan) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Pengaturan", fontSize = 9.sp, maxLines = 1, softWrap = false) }
                            )
                        }

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Tentang,
                            onClick = { viewModel.selectTab(NavTab.Tentang) },
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            label = { Text("Tentang", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = selectedTab, label = "tabCrossfade") { tab ->
                        when (tab) {
                            NavTab.Kasir -> KasirScreen(viewModel = viewModel)
                            NavTab.Produk -> ProdukScreen(viewModel = viewModel)
                            NavTab.Laporan -> LaporanScreen(viewModel = viewModel)
                            NavTab.Kasbon -> KasbonScreen(viewModel = viewModel)
                            NavTab.Pengaturan -> PengaturanScreen(viewModel = viewModel)
                            NavTab.Tentang -> TentangScreen()
                        }
                    }

                    showGuideRole?.let { role ->
                        GuideBookDialog(role = role, onDismiss = { viewModel.closeGuideModal() })
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideBookDialog(role: String, onDismiss: () -> Unit) {
    val isOwner = role == "owner" || role == "pemilik"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isOwner) "📖 Buku Panduan Operasional Owner" else "📖 Buku Panduan Operasional Kasir",
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isOwner) {
                    GuideItem("1. Pengaturan Toko & Metode Bayar", "Atur nama usaha, alamat, no telp/WA, footer struk, URL QRIS toko, dan nomor rekening bank transfer pada menu Pengaturan agar identitas otomatis muncul di Struk & Dokumen PDF.")
                    GuideItem("2. Manajemen User Kasir & Hak Akses", "Buat akun untuk kasir toko Anda dengan role 'Kasir' dan simpan password secara aman untuk memisahkan hak akses.")
                    GuideItem("3. Kelola Produk, Varian & Topping", "Tambah produk baru, kelola kategori kustom, harga jual dasar, harga grosir bertingkat, stok real-time, varian, dan topping tambahan pada menu Produk.")
                    GuideItem("4. Hitung HPP & Resep BOM Bahan Baku", "Gunakan modul Kalkulator HPP & Resep BOM untuk menghitung biaya modal per unit menu secara presisi berdasarkan bahan baku (kulakan) dan estimasi waste. Simpan dan cetak/ekspor dokumen PDF Lembar Resep HPP.")
                    GuideItem("5. Evaluasi Laporan Keuangan & Cetak PDF", "Pantau Omset Kotor, Estimasi Modal HPP, Laba Bersih, Rata-rata Pembelian (AOV), dan Filter Tanggal Kustom WIB. Ekspor/Cetak Laporan Penjualan Dokumen PDF resmi ukuran A4 yang siap disimpan atau dikirim.")
                    GuideItem("6. Kelola Piutang & Cetak Laporan Kasbon PDF", "Pantau daftar kasbon aktif, umur tunggakan (aging status), serta cetak/ekspor Laporan Piutang & Kasbon Pelanggan dalam format PDF.")
                    GuideItem("7. Cadangkan & Pulihkan (Backup & Restore)", "Unduh file cadangan data JSON SQLite ke Folder Download atau bagikan ke Google Drive/WhatsApp secara berkala pada menu Pengaturan untuk memastikan keamanan database.")
                } else {
                    GuideItem("1. Login Akun Kasir", "Masuk dengan username dan password kasir yang telah disiapkan oleh Pemilik toko.")
                    GuideItem("2. Buka Shift & Modal Awal Laci", "Buka sesi shift baru dan masukkan nominal Modal Awal Laci di kasir sebelum memulai transaksi pertama harian.")
                    GuideItem("3. Pemesanan & Kasir Cepat", "Pilih produk dari katalog kasir, pilih varian/topping opsional, tentukan tipe pesanan (Dine-In/Takeaway), dan input nomor meja atau nama pelanggan.")
                    GuideItem("4. Tunda Pesanan (Bill Gantung)", "Jika pelanggan menunda bayar, tekan tombol 'Tunda Pesanan' untuk menyimpan keranjang dan melayani antrean pelanggan berikutnya.")
                    GuideItem("5. Pembayaran Cepat & Struk PDF", "Proses pembayaran Tunai (dengan hitung kembalian instan), QRIS, Transfer Bank, atau Kasbon. Cetak struk ke printer thermal Bluetooth atau cetak/simpan struk dalam format PDF.")
                    GuideItem("6. Catat Kas Keluar Operasional", "Catat pengeluaran kasir selama shift (seperti beli es batu, gas, atau perlengkapan darurat) pada menu Laporan Kas Keluar agar pembukuan laci kas tetap seimbang.")
                    GuideItem("7. Tutup Shift & Cetak Laporan Shift PDF", "Di akhir jam kerja, lakukan Tutup Shift, hitung uang kas fisik riil di laci kasir, cek selisih kas (sesuai/kurang/lebih), lalu cetak Dokumen Rekap Shift Kasir PDF sebagai bukti serah terima.")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)) {
                Text("Paham & Tutup")
            }
        }
    )
}

@Composable
private fun GuideItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryIndigo)
        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}
