package com.quezic.ui.screens.settings

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quezic.data.local.ProxyTestResult
import com.quezic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Cookie file picker (kept as fallback)
    val cookieFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadCookies(it) }
    }

    // YouTube Sign-In WebView Dialog
    if (uiState.showYouTubeSignIn) {
        YouTubeSignInDialog(
            onDismiss = { viewModel.hideYouTubeSignIn() },
            onSignInComplete = { viewModel.extractAndUploadCookies() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Gray6,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Gray6
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // YouTube Proxy Section
            SettingsSection(title = "YouTube Proxy") {
                Text(
                    text = "A proxy server is used to enable YouTube audio playback. The default server is provided for convenience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Proxy URL input
                OutlinedTextField(
                    value = uiState.proxyUrl,
                    onValueChange = { viewModel.updateProxyUrl(it) },
                    label = { Text("Proxy URL") },
                    placeholder = { Text("http://your-server:3000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.saveProxyUrl()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        focusedLabelColor = AccentGreen,
                        cursorColor = AccentGreen,
                        unfocusedBorderColor = Gray3,
                        unfocusedLabelColor = Gray2,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (uiState.proxyUrl.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateProxyUrl("") }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = Gray2
                                )
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Enable/disable switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable proxy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Switch(
                        checked = uiState.proxyEnabled,
                        onCheckedChange = { viewModel.toggleProxyEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentGreen,
                            uncheckedThumbColor = Gray2,
                            uncheckedTrackColor = Gray4
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Test and Save buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testProxy() },
                        enabled = uiState.proxyUrl.isNotBlank() && !uiState.isTestingProxy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentGreen
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AccentGreen)
                        )
                    ) {
                        if (uiState.isTestingProxy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Rounded.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test")
                    }
                    
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.saveProxyUrl()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
                
                // Reset to default button
                TextButton(
                    onClick = { viewModel.resetToDefaultProxy() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Gray2
                    )
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset to default")
                }
                
                // Test result
                AnimatedVisibility(visible = uiState.proxyTestResult != null) {
                    val result = uiState.proxyTestResult
                    val (bgColor, textColor, text) = when (result) {
                        is ProxyTestResult.Success -> Triple(
                            SystemGreen.copy(alpha = 0.2f),
                            SystemGreen,
                            "Connection successful!"
                        )
                        is ProxyTestResult.Error -> Triple(
                            SystemRed.copy(alpha = 0.2f),
                            SystemRed,
                            "Error: ${result.message}"
                        )
                        null -> Triple(Color.Transparent, Color.White, "")
                    }
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (result is ProxyTestResult.Success) 
                                    Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    }
                }
                
                // Help section
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { viewModel.toggleProxyHelp() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SystemBlue
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.showProxyHelp) 
                            Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("How to set up a proxy server")
                }
                
                AnimatedVisibility(visible = uiState.showProxyHelp) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Gray5,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "About the proxy:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            
                            Text(
                                text = "The default proxy server is provided for your convenience. If you prefer to host your own, follow these steps:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray1,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            HelpStep(
                                number = "1",
                                text = "Push the 'proxy-server' folder to GitHub"
                            )
                            HelpStep(
                                number = "2",
                                text = "Deploy to Railway, Render, or Fly.io (free tier available)"
                            )
                            HelpStep(
                                number = "3",
                                text = "Or run locally with Docker: docker compose up -d"
                            )
                            HelpStep(
                                number = "4",
                                text = "Enter your server's URL above"
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "The proxy uses yt-dlp to extract YouTube audio streams reliably.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray2
                            )
                        }
                    }
                }
            }
            
            // YouTube Authentication Section
            SettingsSection(title = "YouTube Authentication") {
                Text(
                    text = "Sign in to your Google account to authenticate YouTube requests. This helps avoid rate limiting and access restrictions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Cookie status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Authentication status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isCheckingCookies) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            val (statusColor, statusText) = when (uiState.cookieStatus) {
                                CookieStatus.Configured -> Pair(SystemGreen, "Signed in")
                                CookieStatus.NotConfigured -> Pair(SystemOrange, "Not signed in")
                                CookieStatus.Unknown -> Pair(Gray2, "Unknown")
                            }
                            Icon(
                                imageVector = when (uiState.cookieStatus) {
                                    CookieStatus.Configured -> Icons.Rounded.CheckCircle
                                    CookieStatus.NotConfigured -> Icons.Rounded.Warning
                                    CookieStatus.Unknown -> Icons.Rounded.HelpOutline
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sign in and Sign out buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.showYouTubeSignIn() },
                        enabled = !uiState.isUploadingCookies && uiState.proxyUrl.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0000), // YouTube red
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isUploadingCookies) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in to YouTube")
                    }

                    OutlinedButton(
                        onClick = { viewModel.deleteCookies() },
                        enabled = !uiState.isUploadingCookies && uiState.cookieStatus == CookieStatus.Configured,
                        modifier = Modifier.weight(0.6f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SystemRed
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (uiState.cookieStatus == CookieStatus.Configured) SystemRed else Gray3
                            )
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign out")
                    }
                }

                // Upload file fallback
                TextButton(
                    onClick = { cookieFileLauncher.launch("*/*") },
                    enabled = !uiState.isUploadingCookies && uiState.proxyUrl.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Gray2
                    )
                ) {
                    Icon(
                        Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Or upload cookies.txt manually")
                }

                // Refresh status button
                TextButton(
                    onClick = { viewModel.checkCookieStatus() },
                    enabled = !uiState.isCheckingCookies,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SystemBlue
                    )
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh status")
                }

                // Cookie message
                AnimatedVisibility(visible = uiState.cookieMessage != null) {
                    val bgColor = if (uiState.cookieMessageIsError) SystemRed.copy(alpha = 0.2f) else SystemGreen.copy(alpha = 0.2f)
                    val textColor = if (uiState.cookieMessageIsError) SystemRed else SystemGreen

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.cookieMessageIsError) Icons.Rounded.Error else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.cookieMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Xiaomi/MIUI Lock Screen Fix Section
            // Only show on Xiaomi devices
            if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("POCO", ignoreCase = true)
            ) {
                XiaomiLockScreenSection()
            }

            // About Section
            SettingsSection(title = "About") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", color = Gray2)
                    Text("1.0.0", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray5)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun HelpStep(
    number: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = RoundedCornerShape(4.dp),
            color = AccentGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Gray1
        )
    }
}

/**
 * Xiaomi/MIUI-specific section for fixing lock screen media controls.
 * Guides the user through enabling autostart, lock screen notifications,
 * and disabling battery optimization.
 */
@Composable
private fun XiaomiLockScreenSection() {
    val context = LocalContext.current
    
    SettingsSection(title = "Lock Screen Controls") {
        Text(
            text = "Xiaomi/MIUI may block lock screen music controls by default. Follow these steps to enable them:",
            style = MaterialTheme.typography.bodySmall,
            color = Gray2,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Step 1: Autostart
        XiaomiSettingsStep(
            number = "1",
            title = "Enable Autostart",
            description = "Allows Quezic to run in the background and keep music playing.",
            buttonText = "Open Autostart Settings",
            onClick = {
                try {
                    val intent = Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback for HyperOS or different MIUI versions
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step 2: Lock Screen Notifications
        XiaomiSettingsStep(
            number = "2",
            title = "Enable Lock Screen Notifications",
            description = "Shows music controls on your lock screen.",
            buttonText = "Open Notification Settings",
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step 3: Battery Optimization
        XiaomiSettingsStep(
            number = "3",
            title = "Disable Battery Optimization",
            description = "Prevents the system from killing Quezic while music is playing.",
            buttonText = "Open Battery Settings",
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Additional tip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SystemBlue.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = SystemBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "In notification settings, make sure 'Music Playback' channel is enabled and 'Show on lock screen' is turned on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemBlue
                )
            }
        }
    }
}

@Composable
private fun XiaomiSettingsStep(
    number: String,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(6.dp),
            color = AccentGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Gray2,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentGreen
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AccentGreen)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Full-screen dialog that opens a WebView to youtube.com for Google sign-in.
 * After the user signs in, cookies are extracted from CookieManager and uploaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YouTubeSignInDialog(
    onDismiss: () -> Unit,
    onSignInComplete: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }
    var isSignedIn by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Gray6
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Sign in to YouTube",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (currentUrl.isNotBlank()) {
                                Text(
                                    text = currentUrl.take(50) + if (currentUrl.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray2,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = onSignInComplete,
                            enabled = isSignedIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Done")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Gray5,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )

                // Loading indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF0000) // YouTube red
                    )
                }

                // WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Enable cookies
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let { currentUrl = it }

                                    // Flush cookies to ensure they're persisted
                                    cookieManager.flush()

                                    // Check Google auth cookies (most reliable indicator)
                                    val googleCookies = cookieManager.getCookie("https://www.google.com") ?: ""
                                    val ytCookies = cookieManager.getCookie("https://www.youtube.com") ?: ""
                                    isSignedIn = googleCookies.contains("SID=") ||
                                        googleCookies.contains("SAPISID=") ||
                                        googleCookies.contains("__Secure-1PSID=") ||
                                        ytCookies.contains("LOGIN_INFO=") ||
                                        ytCookies.contains("SAPISID=")
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    // Allow all navigation within Google/YouTube domains
                                    return false
                                }
                            }

                            webChromeClient = WebChromeClient()

                            // Start at YouTube accounts page
                            loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
