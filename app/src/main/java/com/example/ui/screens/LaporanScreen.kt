package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashExpenseEntity
import com.example.data.model.ShiftEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DateTimeHelper
import com.example.util.PdfReportHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun LaporanScreen(viewModel: PosViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser?.role == "kasir") {
        KasirShiftReportSection(viewModel)
    } else {
        OwnerStoreReportSection(viewModel)
    }
}

@Composable
private fun KasirShiftReportSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val allShifts by viewModel.shifts.collectAsState()

    var expKet by remember { mutableStateOf("") }
    var expAmount by remember { mutableStateOf("") }
    var showEndShiftDialog by remember { mutableStateOf(false) }
    var showOpenShiftDialog by remember { mutableStateOf(false) }

    // Filter shifts for this cashier
    val myShifts = remember(allShifts, currentUser) {
        allShifts.filter { it.kasirUsername == (currentUser?.username ?: "") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeShift == null) {
            // === NO ACTIVE SHIFT: Prompt to Open Shift ===
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarningAmber.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = WarningAmber)
                            Text(
                                "Belum Ada Sesi Shift yang Berjalan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = WarningAmber
                            )
                        }
                    }

                    Text(
                        "Halo, ${currentUser?.nama ?: "Kasir"}! Silakan mulai sesi shift baru sebelum melayani transaksi agar laporan dan uang laci tercatat rapi.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    var modalInput by remember { mutableStateOf(storeSettings.modalAwalLaci.toInt().toString()) }
                    var noteInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = modalInput,
                        onValueChange = { modalInput = it },
                        label = { Text("Modal Awal Uang Laci (Rp)") },
                        leadingIcon = { Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Catatan Pembuka Shift (Opsional)") },
                        placeholder = { Text("Misal: Shift Pagi / Kasir Utama") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val modal = modalInput.toDoubleOrNull() ?: storeSettings.modalAwalLaci
                            viewModel.openShift(modal, noteInput)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Shift Baru Sekarang", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        } else {
            // === ACTIVE SHIFT ACTIVE ===
            val currentActiveShift = activeShift!!
            val shiftTrx = remember(transactions, currentActiveShift) {
                transactions.filter { it.shiftId == currentActiveShift.shiftCode }
            }
            val shiftExpenses = remember(cashExpenses, currentActiveShift) {
                cashExpenses.filter { it.shiftId == currentActiveShift.shiftCode }
            }

            val omsetTunai = shiftTrx.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan }
            val omsetDigital = shiftTrx.filter { it.metode == "qris" || it.metode == "transfer" }.sumOf { it.totalPemasukan }
            val omsetKasbon = shiftTrx.filter { it.metode == "kasbon" }.sumOf { it.totalPemasukan }
            val totalExpenses = shiftExpenses.sumOf { it.nominal }
            val totalOmset = shiftTrx.sumOf { it.totalPemasukan }
            val expectedPhysicalCash = currentActiveShift.modalAwal + omsetTunai - totalExpenses

            // Active Shift Status Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessEmerald.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessEmerald
                            ) {
                                Text(
                                    "🟢 SHIFT AKTIF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(currentActiveShift.shiftCode, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = {
                                PdfReportHelper.generateAndPrintShiftReport(
                                    context = context,
                                    storeName = storeSettings.namaToko,
                                    kasirName = currentActiveShift.kasirNama,
                                    modalLaci = currentActiveShift.modalAwal,
                                    transactions = shiftTrx,
                                    expenses = shiftExpenses,
                                    shiftCode = currentActiveShift.shiftCode,
                                    waktuBukaStr = currentActiveShift.waktuBukaStr
                                )
                            }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak", tint = PrimaryIndigo)
                        }
                    }

                    Divider(color = SuccessEmerald.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Kasir Operator:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text(currentActiveShift.kasirNama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Waktu Buka Shift:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text(DateTimeHelper.cleanWib(currentActiveShift.waktuBukaStr), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Cash Expenses (Kas Keluar) Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💸 Kas Keluar Operasional Shift Ini", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expKet,
                            onValueChange = { expKet = it },
                            placeholder = { Text("Ket (Misal: Es Batu, Gas)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = expAmount,
                            onValueChange = { expAmount = it },
                            placeholder = { Text("Rp", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                val nom = expAmount.toDoubleOrNull() ?: 0.0
                                if (expKet.isNotBlank() && nom > 0) {
                                    viewModel.addCashExpense(expKet, nom)
                                    expKet = ""
                                    expAmount = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Keluar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (shiftExpenses.isEmpty()) {
                        Text(
                            "Belum ada kas keluar pada shift ini.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        shiftExpenses.forEach { exp ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.keterangan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(exp.tanggalStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "- Rp ${viewModel.formatRupiah(exp.nominal)}",
                                        style = PriceTextStyle.copy(fontSize = 12.sp, color = DangerRed)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteCashExpense(exp.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = DangerRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            // Shift Omset Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📊 Ringkasan Omset Shift Berjalan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)

                    Spacer(modifier = Modifier.height(4.dp))

                    ReportRow("Modal Awal Laci:", "Rp ${viewModel.formatRupiah(currentActiveShift.modalAwal)}")
                    ReportRow("Total Transaksi Terlayani:", "${shiftTrx.size} Transaksi")
                    ReportRow("Omset Tunai (Cash):", "Rp ${viewModel.formatRupiah(omsetTunai)}", SuccessEmerald)
                    ReportRow("Omset Digital (QRIS/Transfer):", "Rp ${viewModel.formatRupiah(omsetDigital)}", PrimaryIndigo)
                    ReportRow("Omset Kasbon:", "Rp ${viewModel.formatRupiah(omsetKasbon)}", DangerRed)
                    ReportRow("Total Kas Keluar:", "- Rp ${viewModel.formatRupiah(totalExpenses)}", DangerRed)

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    ReportRow("Total Omset Shift:", "Rp ${viewModel.formatRupiah(totalOmset)}", isBold = true)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessEmerald.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("💵 Uang Fisik Kas Seharusnya di Laci:", fontSize = 11.sp, color = SuccessEmerald, fontWeight = FontWeight.Bold)
                            Text(
                                "Rp ${viewModel.formatRupiah(expectedPhysicalCash)}",
                                style = PriceTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SuccessEmerald)
                            )
                            Text(
                                "(Modal Awal + Tunai Masuk - Kas Keluar)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                PdfReportHelper.generateAndPrintShiftReport(
                                    context = context,
                                    storeName = storeSettings.namaToko,
                                    kasirName = currentActiveShift.kasirNama,
                                    modalLaci = currentActiveShift.modalAwal,
                                    transactions = shiftTrx,
                                    expenses = shiftExpenses,
                                    shiftCode = currentActiveShift.shiftCode,
                                    waktuBukaStr = currentActiveShift.waktuBukaStr
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak PDF", fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { showEndShiftDialog = true },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tutup Shift", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // End Shift Dialog
            if (showEndShiftDialog) {
                var actualCashInput by remember { mutableStateOf(expectedPhysicalCash.toInt().toString()) }
                var closeNoteInput by remember { mutableStateOf("") }

                val actualCash = actualCashInput.toDoubleOrNull() ?: 0.0
                val selisih = actualCash - expectedPhysicalCash

                AlertDialog(
                    onDismissRequest = { showEndShiftDialog = false },
                    title = {
                        Text(
                            "🔒 Serah Terima & Tutup Shift",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Silakan hitung uang tunai fisik di laci kasir untuk rekonsiliasi akhir sesi shift.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Modal Awal:", fontSize = 11.sp)
                                        Text("Rp ${viewModel.formatRupiah(currentActiveShift.modalAwal)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Omset Tunai:", fontSize = 11.sp)
                                        Text("+ Rp ${viewModel.formatRupiah(omsetTunai)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SuccessEmerald)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Kas Keluar:", fontSize = 11.sp)
                                        Text("- Rp ${viewModel.formatRupiah(totalExpenses)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DangerRed)
                                    }
                                    Divider()
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Uang Fisik Seharusnya:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Rp ${viewModel.formatRupiah(expectedPhysicalCash)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryIndigo)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = actualCashInput,
                                onValueChange = { actualCashInput = it },
                                label = { Text("Uang Fisik Dihitung Kasir (Rp)") },
                                leadingIcon = { Text("Rp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Discrepancy indicator
                            val selisihColor = when {
                                selisih > 0 -> PrimaryIndigo
                                selisih < 0 -> DangerRed
                                else -> SuccessEmerald
                            }
                            val selisihText = when {
                                selisih > 0 -> "🔵 Lebih (+ Rp ${viewModel.formatRupiah(selisih)})"
                                selisih < 0 -> "🔴 Kurang (- Rp ${viewModel.formatRupiah(Math.abs(selisih))})"
                                else -> "🟢 Pas / Sesuai (Rp 0)"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = selisihColor.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status Selisih Kas:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(selisihText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = selisihColor)
                                }
                            }

                            OutlinedTextField(
                                value = closeNoteInput,
                                onValueChange = { closeNoteInput = it },
                                label = { Text("Catatan Serah Terima / Alasan Selisih") },
                                placeholder = { Text("Misal: Kas pas, uang receh siap untuk shift berikutnya") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val nowFormatted = DateTimeHelper.formatDateTimeWib(Date())
                                viewModel.closeShift(
                                    uangFisikAktual = actualCash,
                                    catatan = closeNoteInput
                                ) { closedShift ->
                                    PdfReportHelper.generateAndPrintShiftReport(
                                        context = context,
                                        storeName = storeSettings.namaToko,
                                        kasirName = closedShift.kasirNama,
                                        modalLaci = closedShift.modalAwal,
                                        transactions = shiftTrx,
                                        expenses = shiftExpenses,
                                        shiftCode = closedShift.shiftCode,
                                        waktuBukaStr = closedShift.waktuBukaStr,
                                        waktuTutupStr = nowFormatted,
                                        uangFisikAktual = actualCash,
                                        selisihKas = selisih,
                                        catatan = closeNoteInput
                                    )
                                }
                                showEndShiftDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text("Kunci & Tutup Shift", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndShiftDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }

        // === PAST SHIFTS HISTORY FOR CASHIER ===
        val closedShifts = remember(myShifts) {
            myShifts.filter { it.status == "CLOSED" }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📜 Riwayat Shift Anda Sebelumnya", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SecondaryViolet)

                if (closedShifts.isEmpty()) {
                    Text(
                        "Belum ada arsip sesi shift yang ditutup.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    closedShifts.forEach { shift ->
                        ShiftHistoryCard(
                            shift = shift,
                            viewModel = viewModel,
                            transactions = transactions,
                            cashExpenses = cashExpenses,
                            storeName = storeSettings.namaToko,
                            onDelete = null // Kasir cannot delete shifts
                        )
                    }
                }
            }
        }
    }
}

private enum class ReportPeriod { Harian, Mingguan, Bulanan, EvaluasiKustom, AuditShift, ArsipHpp }

private data class ProductSalesSummary(
    val nama: String,
    val qty: Int,
    val totalOmset: Double,
    val totalModal: Double = 0.0,
    val totalProfit: Double = 0.0,
    val marginPct: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerStoreReportSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val hppHistoryList by viewModel.hppHistoryList.collectAsState()
    val products by viewModel.products.collectAsState()
    val allShifts by viewModel.shifts.collectAsState()

    // Search, method filter, and pagination states
    var searchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf("Semua") }
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 15

    // Calculate time bounds
    val now = remember { System.currentTimeMillis() }
    val oneDayMs = 24 * 60 * 60 * 1000L

    val todayCalendar = Calendar.getInstance(DateTimeHelper.TIMEZONE_WIB).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfTodayMs = todayCalendar.timeInMillis

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.Harian) }
    var customStartDateMs by remember { mutableStateOf(startOfTodayMs - (29 * oneDayMs)) }
    var customEndDateMs by remember { mutableStateOf(now) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, selectedMethodFilter, selectedPeriod, customStartDateMs, customEndDateMs) {
        currentPage = 1
    }

    val filteredTransactions = remember(transactions, selectedPeriod, customStartDateMs, customEndDateMs) {
        when (selectedPeriod) {
            ReportPeriod.Harian -> transactions.filter { it.waktu >= startOfTodayMs }
            ReportPeriod.Mingguan -> transactions.filter { it.waktu >= (startOfTodayMs - (6 * oneDayMs)) }
            ReportPeriod.Bulanan -> transactions.filter { it.waktu >= (startOfTodayMs - (29 * oneDayMs)) }
            ReportPeriod.EvaluasiKustom -> transactions.filter { it.waktu >= customStartDateMs && it.waktu <= (customEndDateMs.coerceAtLeast(customStartDateMs) + oneDayMs - 1) }
            ReportPeriod.AuditShift, ReportPeriod.ArsipHpp -> transactions
        }
    }

    // Apply specific text search & payment method filters
    val searchFilteredTransactions = remember(filteredTransactions, searchQuery, selectedMethodFilter) {
        filteredTransactions.filter { trx ->
            val matchesSearch = searchQuery.isBlank() ||
                    trx.id.contains(searchQuery, ignoreCase = true) ||
                    trx.kasir.contains(searchQuery, ignoreCase = true) ||
                    trx.pelanggan.contains(searchQuery, ignoreCase = true) ||
                    trx.nomorMeja.contains(searchQuery, ignoreCase = true) ||
                    trx.itemsJson.contains(searchQuery, ignoreCase = true)

            val matchesMethod = selectedMethodFilter == "Semua" ||
                    trx.metode.equals(selectedMethodFilter, ignoreCase = true)

            matchesSearch && matchesMethod
        }
    }

    val filteredExpenses = remember(cashExpenses, selectedPeriod, customStartDateMs, customEndDateMs) {
        val sdfIsoWib = DateTimeHelper.createFormatter("yyyy-MM-dd", Locale.US)
        when (selectedPeriod) {
            ReportPeriod.Harian -> cashExpenses.filter {
                val nowStr = DateTimeHelper.formatIsoDateWib(Date())
                it.tanggalISO == nowStr
            }
            ReportPeriod.Mingguan -> cashExpenses.filter {
                val pastMs = startOfTodayMs - (6 * oneDayMs)
                try {
                    val date = sdfIsoWib.parse(it.tanggalISO)
                    date != null && date.time >= pastMs
                } catch (e: Exception) { true }
            }
            ReportPeriod.Bulanan -> cashExpenses.filter {
                val pastMs = startOfTodayMs - (29 * oneDayMs)
                try {
                    val date = sdfIsoWib.parse(it.tanggalISO)
                    date != null && date.time >= pastMs
                } catch (e: Exception) { true }
            }
            ReportPeriod.EvaluasiKustom -> cashExpenses.filter {
                try {
                    val date = sdfIsoWib.parse(it.tanggalISO)
                    date != null && date.time in customStartDateMs..(customEndDateMs + oneDayMs - 1)
                } catch (e: Exception) { true }
            }
            ReportPeriod.AuditShift, ReportPeriod.ArsipHpp -> cashExpenses
        }
    }

    // Metrics for standard periods
    val totalOmset = searchFilteredTransactions.sumOf { it.totalPemasukan }
    val totalModal = searchFilteredTransactions.sumOf { it.totalModal }
    val totalKasKeluar = filteredExpenses.sumOf { it.nominal }
    val labaKotor = totalOmset - totalModal
    val labaBersih = labaKotor - totalKasKeluar
    val profitMarginPct = if (totalOmset > 0) (labaBersih / totalOmset) * 100.0 else 0.0

    val omsetTunai = searchFilteredTransactions.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan }
    val omsetQris = searchFilteredTransactions.filter { it.metode == "qris" }.sumOf { it.totalPemasukan }
    val omsetTransfer = searchFilteredTransactions.filter { it.metode == "transfer" }.sumOf { it.totalPemasukan }
    val omsetKasbon = searchFilteredTransactions.filter { it.metode == "kasbon" }.sumOf { it.totalPemasukan }

    val countDineIn = searchFilteredTransactions.count { it.tipePesanan == "Dine-In" }
    val countTakeaway = searchFilteredTransactions.count { it.tipePesanan != "Dine-In" }
    val totalTrxCount = searchFilteredTransactions.size
    val averageBasket = if (totalTrxCount > 0) totalOmset / totalTrxCount else 0.0

    val daysInPeriod = when (selectedPeriod) {
        ReportPeriod.Harian -> 1.0
        ReportPeriod.Mingguan -> 7.0
        ReportPeriod.Bulanan -> 30.0
        ReportPeriod.EvaluasiKustom -> (((customEndDateMs - customStartDateMs) / oneDayMs) + 1.0).coerceAtLeast(1.0)
        ReportPeriod.AuditShift, ReportPeriod.ArsipHpp -> 1.0
    }
    val avgDailyOmset = totalOmset / daysInPeriod
    val avgDailyNetProfit = labaBersih / daysInPeriod

    // Top products
    val topProducts = remember(searchFilteredTransactions, products) {
        val mapQty = mutableMapOf<String, Int>()
        val mapOmset = mutableMapOf<String, Double>()
        val mapModal = mutableMapOf<String, Double>()

        searchFilteredTransactions.forEach { trx ->
            try {
                val arr = org.json.JSONArray(trx.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("nama")
                    val qty = obj.getInt("qty")
                    val sub = obj.getDouble("subtotal")
                    val modal = obj.optDouble("modal", 0.0) * qty
                    mapQty[name] = (mapQty[name] ?: 0) + qty
                    mapOmset[name] = (mapOmset[name] ?: 0.0) + sub
                    mapModal[name] = (mapModal[name] ?: 0.0) + modal
                }
            } catch (e: Exception) {}
        }
        mapQty.map { (name, qty) ->
            val omset = mapOmset[name] ?: 0.0
            val modal = mapModal[name] ?: 0.0
            val profit = omset - modal
            val margin = if (omset > 0) (profit / omset) * 100.0 else 0.0
            ProductSalesSummary(name, qty, omset, modal, profit, margin)
        }
        .sortedByDescending { it.qty }
        .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Report Period Tabs with swipe hint
        Text(
            "💡 Geser tab ke samping untuk melihat laporan shift kasir & evaluasi",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
        )
        SecondaryScrollableTabRow(selectedTabIndex = selectedPeriod.ordinal) {
            Tab(
                selected = selectedPeriod == ReportPeriod.Harian,
                onClick = { selectedPeriod = ReportPeriod.Harian },
                text = { Text("📅 Hari Ini", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.Mingguan,
                onClick = { selectedPeriod = ReportPeriod.Mingguan },
                text = { Text("📆 7 Hari", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.Bulanan,
                onClick = { selectedPeriod = ReportPeriod.Bulanan },
                text = { Text("🗓️ 30 Hari", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.EvaluasiKustom,
                onClick = { selectedPeriod = ReportPeriod.EvaluasiKustom },
                text = { Text("🔍 Kustom", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.AuditShift,
                onClick = { selectedPeriod = ReportPeriod.AuditShift },
                text = { Text("📋 Audit Shift", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.ArsipHpp,
                onClick = { selectedPeriod = ReportPeriod.ArsipHpp },
                text = { Text("📚 Arsip HPP", fontSize = 11.sp) }
            )
        }

        if (selectedPeriod == ReportPeriod.AuditShift) {
            // === OWNER SHIFT AUDIT CENTER ===
            OwnerShiftAuditSection(
                viewModel = viewModel,
                allShifts = allShifts,
                transactions = transactions,
                cashExpenses = cashExpenses,
                storeName = storeSettings.namaToko
            )
        } else if (selectedPeriod == ReportPeriod.ArsipHpp) {
            // === ARSIP HPP SECTION ===
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📚 Arsip Riwayat HPP & Costing Resep", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SecondaryViolet)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hppHistoryList.isEmpty()) {
                        Text("Belum ada arsip riwayat HPP.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    } else {
                        hppHistoryList.forEach { hpp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hpp.namaProduk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("HPP: Rp ${viewModel.formatRupiah(hpp.hppFinal)} | Target FC: Rp ${viewModel.formatRupiah(hpp.saranTargetFC)} | ${hpp.waktuStr}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        PdfReportHelper.printHppReport(
                                            context = context,
                                            storeName = storeSettings.namaToko,
                                            currentUser = currentUser?.nama ?: "Admin",
                                            namaProduk = hpp.namaProduk,
                                            jumlahUnit = hpp.jumlahUnitFinal,
                                            totalBahan = hpp.totalBahan,
                                            totalBiayaLain = hpp.totalBiayaLain,
                                            tenagaKerja = 0.0,
                                            overhead = hpp.totalBiayaLain,
                                            hppUnit = hpp.hppFinal,
                                            targetFcPct = hpp.targetFCPersen,
                                            saranTargetFc = hpp.saranTargetFC,
                                            m35 = hpp.m35,
                                            m50 = hpp.m50,
                                            customPrice = hpp.saranTargetFC,
                                            bahanListJson = hpp.bahanListJson
                                        )
                                    }) {
                                        Text("Cetak PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                    }
                                    IconButton(onClick = { viewModel.deleteHppHistory(hpp.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        } else {
            // === MAIN ANALYTICS VIEW ===
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("🔍 Cari No. Trx, Kasir, Pelanggan, Meja, Produk...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMethodFilter == "Semua",
                        onClick = { selectedMethodFilter = "Semua" },
                        label = { Text("Semua Metode", fontSize = 11.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = selectedMethodFilter == "tunai",
                        onClick = { selectedMethodFilter = "tunai" },
                        label = { Text("💵 Tunai", fontSize = 11.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = selectedMethodFilter == "qris",
                        onClick = { selectedMethodFilter = "qris" },
                        label = { Text("📱 QRIS", fontSize = 11.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = selectedMethodFilter == "transfer",
                        onClick = { selectedMethodFilter = "transfer" },
                        label = { Text("🏦 Transfer", fontSize = 11.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = selectedMethodFilter == "kasbon",
                        onClick = { selectedMethodFilter = "kasbon" },
                        label = { Text("📝 Kasbon", fontSize = 11.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Active Filter Alert Banner
                if (searchQuery.isNotBlank() || selectedMethodFilter != "Semua") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryIndigo.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔎 Filter Aktif: ${if (searchQuery.isNotBlank()) "\"$searchQuery\"" else ""} ${if (selectedMethodFilter != "Semua") "[${selectedMethodFilter.uppercase()}]" else ""} ($totalTrxCount Trx)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedMethodFilter = "Semua"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Reset Filter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val dateFormatter = remember { DateTimeHelper.createFormatter("dd MMM yyyy") }
                val startStr = dateFormatter.format(Date(customStartDateMs))
                val endStr = dateFormatter.format(Date(customEndDateMs))
                val customDaysCount = (((customEndDateMs - customStartDateMs) / oneDayMs) + 1.0).coerceAtLeast(1.0).toInt()

                if (showDateRangeDialog) {
                    val dateRangePickerState = rememberDateRangePickerState(
                        initialSelectedStartDateMillis = customStartDateMs,
                        initialSelectedEndDateMillis = customEndDateMs
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDateRangeDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    dateRangePickerState.selectedStartDateMillis?.let { startUtc ->
                                        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = startUtc }
                                        val localCal = java.util.Calendar.getInstance().apply {
                                            set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH), utcCal.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        customStartDateMs = localCal.timeInMillis
                                    }
                                    dateRangePickerState.selectedEndDateMillis?.let { endUtc ->
                                        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = endUtc }
                                        val localCal = java.util.Calendar.getInstance().apply {
                                            set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH), utcCal.get(java.util.Calendar.DAY_OF_MONTH), 23, 59, 59)
                                            set(java.util.Calendar.MILLISECOND, 999)
                                        }
                                        customEndDateMs = localCal.timeInMillis
                                    }
                                    showDateRangeDialog = false
                                }
                            ) {
                                Text("Pilih Tanggal", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDateRangeDialog = false }) {
                                Text("Batal")
                            }
                        }
                    ) {
                        DateRangePicker(
                            state = dateRangePickerState,
                            modifier = Modifier.height(400.dp),
                            title = { Text("Rentang Waktu Evaluasi", modifier = Modifier.padding(16.dp)) },
                            showModeToggle = false
                        )
                    }
                }

                // Summary Analytics Card
                val periodTitle = when (selectedPeriod) {
                    ReportPeriod.Harian -> "Hari Ini"
                    ReportPeriod.Mingguan -> "7 Hari Terakhir"
                    ReportPeriod.Bulanan -> "30 Hari Terakhir"
                    ReportPeriod.EvaluasiKustom -> "Periode Kustom"
                    else -> "Evaluasi Toko"
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selectedPeriod == ReportPeriod.EvaluasiKustom) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Rentang Tanggal Evaluasi:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("$startStr s.d. $endStr ($customDaysCount Hari)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Button(
                                    onClick = { showDateRangeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📅 Pilih Kalender", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick preset chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    "7 Hari" to 7L,
                                    "14 Hari" to 14L,
                                    "30 Hari" to 30L,
                                    "60 Hari" to 60L,
                                    "90 Hari" to 90L
                                ).forEach { (label, days) ->
                                    val isSelected = customDaysCount == days.toInt()
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            customEndDateMs = now
                                            customStartDateMs = startOfTodayMs - ((days - 1) * oneDayMs)
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📊 $periodTitle", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SecondaryViolet.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "📦 ${totalTrxCount} Transaksi",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SecondaryViolet,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Divider()

                        ReportRow("Total Omset Kotor:", "Rp ${viewModel.formatRupiah(totalOmset)}", isBold = true)
                        ReportRow("Total Estimasi Modal (HPP):", "- Rp ${viewModel.formatRupiah(totalModal)}", DangerRed)
                        ReportRow("Total Kas Keluar Operasional:", "- Rp ${viewModel.formatRupiah(totalKasKeluar)}", DangerRed)

                        Divider(modifier = Modifier.padding(vertical = 2.dp))

                        ReportRow("Keuntungan Bersih (Net Profit):", "Rp ${viewModel.formatRupiah(labaBersih)}", SuccessEmerald, isBold = true)
                        ReportRow("Profit Margin %:", "${String.format("%.1f", profitMarginPct)}%", if (profitMarginPct >= 20) SuccessEmerald else DangerRed)
                        ReportRow("Rata-rata Omset / Hari:", "Rp ${viewModel.formatRupiah(avgDailyOmset)}")
                        ReportRow("Rata-rata Laba Bersih / Hari:", "Rp ${viewModel.formatRupiah(avgDailyNetProfit)}", SuccessEmerald)
                        ReportRow("Rata-rata Nilai Keranjang (AOV):", "Rp ${viewModel.formatRupiah(averageBasket)}")
                    }
                }

                // Breakdown Pembayaran
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("💳 Breakdown Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)

                        ReportRow("💵 Tunai (Cash):", "Rp ${viewModel.formatRupiah(omsetTunai)}")
                        ReportRow("📱 QRIS Digital:", "Rp ${viewModel.formatRupiah(omsetQris)}", PrimaryIndigo)
                        ReportRow("🏦 Transfer Bank:", "Rp ${viewModel.formatRupiah(omsetTransfer)}")
                        ReportRow("📝 Kasbon Pelanggan:", "Rp ${viewModel.formatRupiah(omsetKasbon)}", DangerRed)

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("🍽️ Tipe Pesanan: Dine-In (${countDineIn}) | Takeaway/Bawa Pulang (${countTakeaway})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Top Products
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔥 5 Menu Terlaris (${periodTitle})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)

                        if (topProducts.isEmpty()) {
                            Text("Belum ada data penjualan.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        } else {
                            topProducts.forEachIndexed { idx, p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${idx + 1}.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Column {
                                            Text(p.nama, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text("${p.qty} Porsi Terjual", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                    }
                                    Text("Rp ${viewModel.formatRupiah(p.totalOmset)}", style = PriceTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                                if (idx < topProducts.size - 1) Divider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }

                // Transactions List
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📋 Riwayat Transaksi Penjualan (${searchFilteredTransactions.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)

                        if (searchFilteredTransactions.isEmpty()) {
                            Text("Tidak ada data transaksi.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        } else {
                            val totalPages = ((searchFilteredTransactions.size - 1) / itemsPerPage) + 1
                            val safeCurrentPage = currentPage.coerceIn(1, totalPages)
                            val startIndex = (safeCurrentPage - 1) * itemsPerPage
                            val pageTransactions = searchFilteredTransactions.drop(startIndex).take(itemsPerPage)

                            pageTransactions.forEachIndexed { index, trx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(trx.id, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            if (trx.shiftId.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PrimaryIndigo.copy(alpha = 0.1f)
                                                ) {
                                                    Text(
                                                        trx.shiftId,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PrimaryIndigo,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text("${trx.tanggalStr} • Kasir: ${trx.kasir} • ${trx.pelanggan}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Rp ${viewModel.formatRupiah(trx.totalPemasukan)}", style = PriceTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (trx.metode.lowercase()) {
                                                "qris" -> PrimaryIndigo.copy(alpha = 0.15f)
                                                "kasbon" -> DangerRed.copy(alpha = 0.15f)
                                                "transfer" -> SecondaryViolet.copy(alpha = 0.15f)
                                                else -> SuccessEmerald.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                trx.metode.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (trx.metode.lowercase()) {
                                                    "qris" -> PrimaryIndigo
                                                    "kasbon" -> DangerRed
                                                    "transfer" -> SecondaryViolet
                                                    else -> SuccessEmerald
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                if (index < pageTransactions.size - 1) Divider(modifier = Modifier.padding(vertical = 6.dp))
                            }

                            // Pagination Controls
                            Divider(modifier = Modifier.padding(top = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { if (safeCurrentPage > 1) currentPage-- },
                                    enabled = safeCurrentPage > 1,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("◀ Seb", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "Hal $safeCurrentPage / $totalPages (${searchFilteredTransactions.size} Trx)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                OutlinedButton(
                                    onClick = { if (safeCurrentPage < totalPages) currentPage++ },
                                    enabled = safeCurrentPage < totalPages,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Lanjut ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                val dateFormatterExport = remember { DateTimeHelper.createFormatter("dd MMM yyyy") }
                val startStrExport = dateFormatterExport.format(Date(customStartDateMs))
                val endStrExport = dateFormatterExport.format(Date(customEndDateMs))

                Button(
                    onClick = {
                        val title = when (selectedPeriod) {
                            ReportPeriod.Harian -> "Penjualan Hari Ini"
                            ReportPeriod.Mingguan -> "Evaluasi 7 Hari Terakhir"
                            ReportPeriod.Bulanan -> "Evaluasi 30 Hari Terakhir"
                            ReportPeriod.EvaluasiKustom -> "Evaluasi Kustom ($startStrExport - $endStrExport)"
                            else -> "Evaluasi Penjualan"
                        }
                        PdfReportHelper.generateAndPrintHtmlReport(
                            context = context,
                            storeName = storeSettings.namaToko,
                            periodName = if (searchQuery.isNotBlank() || selectedMethodFilter != "Semua") "$title (Hasil Filter)" else title,
                            currentUser = currentUser?.nama ?: "Pemilik",
                            transactions = searchFilteredTransactions,
                            expenses = filteredExpenses,
                            topProducts = topProducts.map { Triple(it.nama, it.qty, it.totalOmset) }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak / Ekspor PDF Laporan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// === OWNER SHIFT AUDIT SECTION COMPONENT ===
@Composable
private fun OwnerShiftAuditSection(
    viewModel: PosViewModel,
    allShifts: List<ShiftEntity>,
    transactions: List<TransactionEntity>,
    cashExpenses: List<CashExpenseEntity>,
    storeName: String
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Semua") }
    var selectedKasirFilter by remember { mutableStateOf("Semua") }
    var shiftToDelete by remember { mutableStateOf<ShiftEntity?>(null) }

    val distinctCashiers = remember(allShifts) {
        listOf("Semua") + allShifts.map { it.kasirNama }.distinct()
    }

    val filteredShifts = remember(allShifts, searchQuery, selectedStatusFilter, selectedKasirFilter) {
        allShifts.filter { shift ->
            val matchSearch = searchQuery.isBlank() ||
                    shift.shiftCode.contains(searchQuery, ignoreCase = true) ||
                    shift.kasirNama.contains(searchQuery, ignoreCase = true) ||
                    shift.catatan.contains(searchQuery, ignoreCase = true)

            val matchStatus = when (selectedStatusFilter) {
                "OPEN" -> shift.status == "OPEN"
                "CLOSED" -> shift.status == "CLOSED"
                else -> true
            }

            val matchKasir = selectedKasirFilter == "Semua" || shift.kasirNama == selectedKasirFilter

            matchSearch && matchStatus && matchKasir
        }
    }

    val openCount = allShifts.count { it.status == "OPEN" }
    val closedCount = allShifts.count { it.status == "CLOSED" }
    val totalSelisihAll = allShifts.filter { it.status == "CLOSED" }.sumOf { it.selisihKas }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Sesi Shift", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("${allShifts.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryIndigo)
                    Text("$openCount Aktif • $closedCount Selesai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Audit Selisih Kas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    val selColor = when {
                        totalSelisihAll > 0 -> PrimaryIndigo
                        totalSelisihAll < 0 -> DangerRed
                        else -> SuccessEmerald
                    }
                    Text(
                        "${if (totalSelisihAll > 0) "+" else ""}Rp ${viewModel.formatRupiah(totalSelisihAll)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = selColor
                    )
                    Text(if (totalSelisihAll == 0.0) "Semua Sesuai" else if (totalSelisihAll > 0) "Surplus Laci" else "Defisit Laci", fontSize = 10.sp, color = selColor)
                }
            }
        }

        // Search and Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("🔍 Cari Kode Shift, Kasir, atau Catatan...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter status and cashier chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "Semua",
                onClick = { selectedStatusFilter = "Semua" },
                label = { Text("Semua Status", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == "OPEN",
                onClick = { selectedStatusFilter = "OPEN" },
                label = { Text("🟢 Sedang Aktif ($openCount)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == "CLOSED",
                onClick = { selectedStatusFilter = "CLOSED" },
                label = { Text("🔒 Ditutup ($closedCount)", fontSize = 11.sp) }
            )

            distinctCashiers.forEach { kasir ->
                FilterChip(
                    selected = selectedKasirFilter == kasir,
                    onClick = { selectedKasirFilter = kasir },
                    label = { Text("👤 $kasir", fontSize = 11.sp) }
                )
            }
        }

        // Shifts List
        if (filteredShifts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Tidak ditemukan sesi shift yang sesuai filter.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            filteredShifts.forEach { shift ->
                ShiftHistoryCard(
                    shift = shift,
                    viewModel = viewModel,
                    transactions = transactions,
                    cashExpenses = cashExpenses,
                    storeName = storeName,
                    onDelete = { shiftToDelete = shift }
                )
            }
        }
    }

    // Delete Confirmation Dialog for Owner
    if (shiftToDelete != null) {
        val s = shiftToDelete!!
        AlertDialog(
            onDismissRequest = { shiftToDelete = null },
            title = { Text("Hapus Rekam Sesi Shift?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus arsip shift ${s.shiftCode} (${s.kasirNama})? Data transaksi individual tidak akan terhapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShift(s.id)
                        shiftToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// === REUSABLE SHIFT CARD WITH EXPANDABLE TRANSACTIONS & CASH EXPENSES ===
@Composable
private fun ShiftHistoryCard(
    shift: ShiftEntity,
    viewModel: PosViewModel,
    transactions: List<TransactionEntity>,
    cashExpenses: List<CashExpenseEntity>,
    storeName: String,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val shiftTrx = remember(transactions, shift.shiftCode) {
        transactions.filter { it.shiftId == shift.shiftCode }
    }
    val shiftExp = remember(cashExpenses, shift.shiftCode) {
        cashExpenses.filter { it.shiftId == shift.shiftCode }
    }

    val isOpen = shift.status == "OPEN"
    val omsetTunai = if (isOpen) shiftTrx.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan } else shift.omsetTunai
    val omsetDigital = if (isOpen) shiftTrx.filter { it.metode == "qris" || it.metode == "transfer" }.sumOf { it.totalPemasukan } else shift.omsetDigital
    val totalExp = if (isOpen) shiftExp.sumOf { it.nominal } else shift.totalPengeluaran
    val totalOmset = if (isOpen) shiftTrx.sumOf { it.totalPemasukan } else (omsetTunai + omsetDigital)
    val expectedCash = shift.modalAwal + omsetTunai - totalExp

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOpen) SuccessEmerald else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (isOpen) "🟢 OPEN" else "🔒 CLOSED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isOpen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(shift.shiftCode, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            PdfReportHelper.generateAndPrintShiftReport(
                                context = context,
                                storeName = storeName,
                                kasirName = shift.kasirNama,
                                modalLaci = shift.modalAwal,
                                transactions = shiftTrx,
                                expenses = shiftExp,
                                shiftCode = shift.shiftCode,
                                waktuBukaStr = shift.waktuBukaStr,
                                waktuTutupStr = shift.waktuTutupStr,
                                uangFisikAktual = if (!isOpen) shift.uangFisikAktual else null,
                                selisihKas = if (!isOpen) shift.selisihKas else null,
                                catatan = shift.catatan
                            )
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak PDF", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = DangerRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Timeline & Cashier
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Kasir: ${shift.kasirNama}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Buka: ${DateTimeHelper.cleanWib(shift.waktuBukaStr)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isOpen) "Sesi Masih Aktif" else "Tutup: ${DateTimeHelper.cleanWib(shift.waktuTutupStr)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isOpen) SuccessEmerald else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("${shiftTrx.size} Transaksi", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 2.dp))

            // Financial row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Modal Awal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Rp ${viewModel.formatRupiah(shift.modalAwal)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Omset Tunai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("+Rp ${viewModel.formatRupiah(omsetTunai)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SuccessEmerald)
                }
                Column {
                    Text("Kas Keluar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("-Rp ${viewModel.formatRupiah(totalExp)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Omset", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Rp ${viewModel.formatRupiah(totalOmset)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                }
            }

            // Drawer reconciliation badge (for closed shifts)
            if (!isOpen) {
                val selisih = shift.selisihKas
                val selColor = when {
                    selisih > 0 -> PrimaryIndigo
                    selisih < 0 -> DangerRed
                    else -> SuccessEmerald
                }
                val selLabel = when {
                    selisih > 0 -> "Lebih (+ Rp ${viewModel.formatRupiah(selisih)})"
                    selisih < 0 -> "Kurang (- Rp ${viewModel.formatRupiah(Math.abs(selisih))})"
                    else -> "Pas / Sesuai (Rp 0)"
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = selColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, selColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fisik Laci: Rp ${viewModel.formatRupiah(shift.uangFisikAktual)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(selLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = selColor)
                    }
                }
            }

            if (shift.catatan.isNotBlank()) {
                Text("📝 Catatan: ${shift.catatan}", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }

            // Accordion toggle button
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(if (isExpanded) "Sembunyikan Rincian Shift" else "Lihat ${shiftTrx.size} Transaksi & ${shiftExp.size} Kas Keluar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Expanded Transaction and Expense Details
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider()

                    if (shiftExp.isNotEmpty()) {
                        Text("💸 Rincian Kas Keluar:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DangerRed)
                        shiftExp.forEach { e ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• ${e.keterangan}", fontSize = 11.sp)
                                Text("- Rp ${viewModel.formatRupiah(e.nominal)}", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Text("🛒 Transaksi Terlayani:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                    if (shiftTrx.isEmpty()) {
                        Text("Belum ada transaksi.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        shiftTrx.forEach { t ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(t.id, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${t.tanggalStr} • ${t.metode.uppercase()} • ${t.tipePesanan}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text("Rp ${viewModel.formatRupiah(t.totalPemasukan)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = PriceTextStyle.copy(
                fontSize = 13.sp,
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = color
            )
        )
    }
}
