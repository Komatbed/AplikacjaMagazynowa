package com.example.warehouse

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.model.OcrResult
import com.example.warehouse.ui.components.WasteResultDialog
import com.example.warehouse.ui.screens.CameraScreen
import com.example.warehouse.ui.screens.ConfigScreen
import com.example.warehouse.ui.screens.HomeScreen
import com.example.warehouse.ui.screens.IssueReportScreen
import com.example.warehouse.ui.screens.ManualTakeScreen
import com.example.warehouse.ui.screens.OptimizationScreen
import com.example.warehouse.ui.screens.SettingsScreen
import com.example.warehouse.ui.screens.WarehouseMapScreen
import com.example.warehouse.ui.theme.WarehouseTheme
import com.example.warehouse.ui.viewmodel.InventoryViewModel
import com.example.warehouse.ui.viewmodel.SettingsViewModel
import com.example.warehouse.util.OcrProcessor
import com.example.warehouse.util.ZplPrinter
import com.example.warehouse.ui.screens.InventoryScreen
import com.example.warehouse.ui.screens.ReservedItemsScreen
import com.example.warehouse.ui.screens.WindowCalculatorScreen
import com.example.warehouse.ui.screens.WasteFinderScreen
import com.example.warehouse.ui.screens.HardwarePickerScreen
import com.example.warehouse.ui.screens.CatalogNavigation
import kotlinx.coroutines.launch

import com.example.warehouse.ui.screens.AddInventoryScreen
import com.example.warehouse.ui.screens.AuditLogScreen
import com.example.warehouse.ui.screens.FileOptimizationScreen
import com.example.warehouse.ui.screens.TrainingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Safety check for critical initializations
            try {
                // Pre-check OcrProcessor initialization (often crashes on Samsung if libraries missing)
                com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
            } catch (e: Throwable) {
                Toast.makeText(this, "OCR INIT FAILED: ${e.message}", Toast.LENGTH_LONG).show()
            }

            setContent {
                WarehouseTheme {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    
                    // ViewModels
                    // Safety wrap viewmodel creation
                    val inventoryViewModel: InventoryViewModel = viewModel()
                    val settingsViewModel: SettingsViewModel = viewModel()
                    
                    // Initialize Network with saved URL
                    val apiUrl by settingsViewModel.apiUrl.collectAsState()
                    LaunchedEffect(apiUrl) {
                        NetworkModule.updateUrl(apiUrl)
                    }

                    // OCR Processor - Initialize lazily or safely
                    val ocrProcessorState = remember { mutableStateOf<OcrProcessor?>(null) }
                    LaunchedEffect(Unit) {
                        try {
                            ocrProcessorState.value = OcrProcessor(context)
                        } catch (e: Exception) {
                            // Log error but don't crash
                            e.printStackTrace()
                        }
                    }
                    var ocrResult by remember { mutableStateOf<OcrResult?>(null) }
                    
                    // Offline Status
                    val error by inventoryViewModel.error
                    val isOffline = error?.contains("offline", ignoreCase = true) == true

                    NavHost(navController = navController, startDestination = "home") {
                        
                        // HOME SCREEN
                        composable("home") {
                            HomeScreen(
                                onScanClick = { 
                                    // Always allow navigation to camera, even if OCR might fail later
                                    navController.navigate("camera") 
                                },
                                onManualTakeClick = { navController.navigate("manual_take") },
                                onOptimizationClick = { navController.navigate("optimization") },
                                onSettingsClick = { navController.navigate("settings") },
                                onConfigClick = { navController.navigate("config") },
                                onMapClick = { navController.navigate("map") },
                                onIssueClick = { navController.navigate("issue_report") },
                                onMuntinClick = { navController.navigate("muntin") },
                                onInventoryClick = { navController.navigate("inventory") },
                                onWindowCalcClick = { navController.navigate("window_calc") },
                                onReservationsClick = { navController.navigate("reservations") },
                                onWasteFinderClick = { navController.navigate("waste_finder") },
                                onHardwareClick = { navController.navigate("hardware_picker") },
                                onCatalogClick = { navController.navigate("catalog") },
                                onTrainingClick = { navController.navigate("training") },
                                onAddInventoryClick = { navController.navigate("add_inventory") },
                                onAuditLogClick = { navController.navigate("audit_log") },
                                isOffline = isOffline
                            )
                        }

                        composable("training") {
                            TrainingScreen(onBackClick = { navController.popBackStack() })
                        }

                        composable("add_inventory") {
                            AddInventoryScreen(onBackClick = { navController.popBackStack() })
                        }

                        composable("catalog") {
                            CatalogNavigation(onBackClick = { navController.popBackStack() })
                        }

                        composable("waste_finder") {
                            WasteFinderScreen(onBackClick = { navController.popBackStack() })
                        }

                        composable("hardware_picker") {
                            HardwarePickerScreen(onBackClick = { navController.popBackStack() })
                        }

                        composable("inventory") {
                            InventoryScreen(onBackClick = { navController.popBackStack() })
                        }

                        composable("reservations") {
                            ReservedItemsScreen(onBackClick = { navController.popBackStack() })
                        }

                        composable("window_calc") {
                            WindowCalculatorScreen(onBackClick = { navController.popBackStack() })
                        }

                        // MAP SCREEN
                        composable("map") {
                            WarehouseMapScreen(
                                onBackClick = { navController.popBackStack() },
                                onLocationClick = { _ -> 
                                    // Navigate to ManualTake with location pre-filled
                                    navController.navigate("manual_take")
                                }
                            )
                        }

                        composable("issue_report") {
                            IssueReportScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // CAMERA SCREEN
                        composable("camera") {
                            CameraScreen(
                                onPhotoTaken = { file ->
                                    // Process image
                                    val processor = ocrProcessorState.value
                                    if (processor != null) {
                                        processor.processImage(file) { result ->
                                            ocrResult = result
                                            navController.popBackStack() // Go back to home to show dialog
                                        }
                                    } else {
                                        // Try to init again
                                        try {
                                            val newProcessor = OcrProcessor(context)
                                            ocrProcessorState.value = newProcessor
                                            newProcessor.processImage(file) { result ->
                                                ocrResult = result
                                                navController.popBackStack()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Błąd inicjalizacji OCR: ${e.message}", Toast.LENGTH_LONG).show()
                                            navController.popBackStack()
                                        }
                                    }
                                },
                                onError = { e ->
                                    Toast.makeText(context, "Błąd kamery: $e", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        // OPTIMIZATION SCREEN
                        composable("optimization") {
                            OptimizationScreen(
                                onBackClick = { navController.popBackStack() },
                                onFileOptimizationClick = { navController.navigate("file_optimization") }
                            )
                        }

                        // FILE OPTIMIZATION SCREEN
                        composable("file_optimization") {
                            FileOptimizationScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // MANUAL TAKE SCREEN
                        composable("manual_take") {
                            ManualTakeScreen(
                                onBackClick = { navController.popBackStack() },
                                onShowMessage = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                viewModel = inventoryViewModel
                            )
                        }

                        // SETTINGS SCREEN
                        composable("settings") {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() },
                                onConfigClick = { navController.navigate("config") },
                                onAuditLogClick = { navController.navigate("audit_log") },
                                viewModel = settingsViewModel
                            )
                        }

                        // CONFIG SCREEN
                        composable("config") {
                            ConfigScreen(
                                onBackClick = { navController.popBackStack() },
                                onHistoryClick = { navController.navigate("audit_log") }
                            )
                        }

                        composable("audit_log") {
                            AuditLogScreen(onBackClick = { navController.popBackStack() })
                        }

                        // MUNTIN SCREEN
                        composable("muntin") {
                            com.example.warehouse.ui.screens.MuntinScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }

                    // Global Dialog for OCR Result (if triggered from Camera or Manual Waste)
                    ocrResult?.let { result ->
                        WasteResultDialog(
                            ocrResult = result,
                            onDismiss = { ocrResult = null },
                            onConfirm = { request ->
                                inventoryViewModel.registerWaste(request) {
                                    Toast.makeText(context, "Zapisano w bazie", Toast.LENGTH_SHORT).show()
                                    
                                    // Print Label
                                    scope.launch {
                                        try {
                                            val zpl = ZplPrinter.generateZpl(
                                                profileCode = request.profileCode,
                                                lengthMm = request.lengthMm,
                                                colorName = "${request.internalColor}/${request.externalColor}",
                                                wasteId = "WASTE-${System.currentTimeMillis()}",
                                                location = request.locationLabel
                                            )
                                            
                                            // Use configured IP
                                            val ip = settingsViewModel.printerIp.value
                                            val port = settingsViewModel.printerPort.value
                                            
                                            val printResult = ZplPrinter.printDirectly(ip, port, zpl)
                                            if (printResult.isSuccess) {
                                                Toast.makeText(context, "Wydrukowano etykietę", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val errorMsg = printResult.exceptionOrNull()?.message ?: "Nieznany błąd"
                                                Toast.makeText(context, "Błąd druku: $errorMsg", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                             Toast.makeText(context, "Print Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                ocrResult = null
                            }
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // In a real crash, this might not show if UI thread crashes, but worth a try
            Toast.makeText(this, "CRASH: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
