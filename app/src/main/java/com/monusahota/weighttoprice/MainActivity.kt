package com.monusahota.weighttoprice

import android.os.Bundle
import android.util.Log
import android.content.pm.ApplicationInfo
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.monusahota.weighttoprice.ui.theme.WeightToPriceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        enableEdgeToEdge()
        setContent {
            WeightToPriceTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        ReviewTip(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        )
                    },
                    bottomBar = {
                        BannerAd(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        )
                    }
                ) { innerPadding ->
                    WeightToPriceScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewTip(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pulseTransition = rememberInfiniteTransition(label = "review_tip_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "review_tip_scale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "review_tip_alpha"
    )

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = { openPlayStoreReview(context) },
            modifier = Modifier.graphicsLayer(
                scaleX = pulseScale,
                scaleY = pulseScale,
                alpha = pulseAlpha
            )
        ) {
            Text(
                text = "Do you want any other feature in this app please let me know",
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun openPlayStoreReview(context: android.content.Context) {
    val packageName = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(marketIntent)
    } catch (_: Exception) {
        context.startActivity(webIntent)
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val adWidthDp = with(density) { configuration.screenWidthDp.dp.roundToPx().toDp().value.toInt() }
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val adUnitId = if (isDebuggable) {
        // Google test banner for debug builds (always safe and usually serves)
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        "ca-app-pub-9321276679154460/4420163752"
    }

    val adView = remember(adWidthDp, adUnitId) {
        AdView(context).apply {
            val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
            setAdSize(adaptiveSize)
            setAdUnitId(adUnitId)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("AdMob", "Banner loaded")
                }

                fun onAdFailedToLoad(adError: AdError) {
                    Log.e("AdMob", "Banner failed: ${adError.code} ${adError.message}")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView }
    )
}

@Composable
fun WeightToPriceScreen(modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var calculationMode by remember { mutableStateOf("weightToPrice") } // "weightToPrice" or "priceToWeight"
    var weight by remember { mutableStateOf("") }
    var pricePerKg by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var calculatedPrice by remember { mutableStateOf(0.0) }
    var calculatedWeight by remember { mutableStateOf(0.0) }
    var hasError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Weight ↔ Price",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = calculationMode == "weightToPrice",
                onClick = { 
                    calculationMode = "weightToPrice"
                    // Clear all fields when switching mode
                    weight = ""
                    totalPrice = ""
                    calculatedPrice = 0.0
                    calculatedWeight = 0.0
                    hasError = false
                },
                label = { Text("Weight → Price", fontSize = 14.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = calculationMode == "priceToWeight",
                onClick = { 
                    calculationMode = "priceToWeight"
                    // Clear all fields when switching mode
                    weight = ""
                    totalPrice = ""
                    calculatedPrice = 0.0
                    calculatedWeight = 0.0
                    hasError = false
                },
                label = { Text("Price → Weight", fontSize = 14.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        // Price Per 1kg Input (always shown)
        OutlinedTextField(
            value = pricePerKg,
            onValueChange = { 
                pricePerKg = it
                hasError = false
            },
            label = { Text("Price per 1kg (Rupees)") },
            placeholder = { Text("e.g., 1000") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )

        // Conditional Input Fields based on mode
        if (calculationMode == "weightToPrice") {
            // Weight Input (in grams) - for Weight to Price mode
            OutlinedTextField(
                value = weight,
                onValueChange = { 
                    weight = it
                    hasError = false
                },
                label = { Text("Weight (g)") },
                placeholder = { Text("Enter weight in grams") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        } else {
            // Total Price Input - for Price to Weight mode
            OutlinedTextField(
                value = totalPrice,
                onValueChange = { 
                    totalPrice = it
                    hasError = false
                },
                label = { Text("Price (Rupees)") },
                placeholder = { Text("Enter Price in rupees") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }

        // Calculate Button
        Button(
            onClick = {
                // Hide keyboard when Calculate is clicked
                keyboardController?.hide()
                
                try {
                    val pricePerKgValue = pricePerKg.toDoubleOrNull()
                    
                    if (calculationMode == "weightToPrice") {
                        // Weight to Price calculation
                        val weightValue = weight.toDoubleOrNull()
                        
                        if (weightValue != null && weightValue > 0 && 
                            pricePerKgValue != null && pricePerKgValue >= 0) {
                            // Convert weight from grams to kg
                            val weightInKg = weightValue / 1000.0
                            
                            // Calculate: (weight in kg) × (price per kg)
                            calculatedPrice = weightInKg * pricePerKgValue
                            calculatedWeight = 0.0
                            hasError = false
                        } else {
                            hasError = true
                        }
                    } else {
                        // Price to Weight calculation
                        val totalPriceValue = totalPrice.toDoubleOrNull()
                        
                        if (totalPriceValue != null && totalPriceValue > 0 && 
                            pricePerKgValue != null && pricePerKgValue > 0) {
                            // Calculate: (total price / price per kg) × 1000 = weight in grams
                            val weightInKg = totalPriceValue / pricePerKgValue
                            calculatedWeight = weightInKg * 1000.0 // Convert to grams
                            calculatedPrice = 0.0
                            hasError = false
                        } else {
                            hasError = true
                        }
                    }
                } catch (e: Exception) {
                    hasError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Calculate",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Result Display
        if (hasError) {
            Text(
                text = "Please enter valid numbers",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else if (calculatedPrice > 0 || calculatedWeight > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (calculationMode == "weightToPrice") {
                        // Show price result
                        Text(
                            text = "Total Price",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = String.format("%.2f", calculatedPrice),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Rupees",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        // Show weight result
                        Text(
                            text = "Weight",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = String.format("%.2f", calculatedWeight),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Grams",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeightToPriceScreenPreview() {
    WeightToPriceTheme {
        WeightToPriceScreen()
    }
}

