package com.quezic.ui.screens.settings

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
                    text = "Configure a self-hosted proxy server to bypass YouTube's playback restrictions.",
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
                        focusedBorderColor = SystemPink,
                        focusedLabelColor = SystemPink,
                        cursorColor = SystemPink,
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
                            checkedTrackColor = SystemPink,
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
                            contentColor = SystemPink
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SystemPink)
                        )
                    ) {
                        if (uiState.isTestingProxy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = SystemPink,
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
                            containerColor = SystemPink,
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
                                text = "Setting up your proxy server:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            
                            HelpStep(
                                number = "1",
                                text = "Get a VPS (DigitalOcean, Vultr, etc.) or use your home PC"
                            )
                            HelpStep(
                                number = "2",
                                text = "Install Docker on your server"
                            )
                            HelpStep(
                                number = "3",
                                text = "Copy the 'proxy-server' folder from the Quezic source"
                            )
                            HelpStep(
                                number = "4",
                                text = "Run: docker-compose up -d"
                            )
                            HelpStep(
                                number = "5",
                                text = "Enter your server's URL above (e.g., http://123.45.67.89:3000)"
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "The proxy uses a headless browser to generate valid YouTube authentication tokens.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray2
                            )
                        }
                    }
                }
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
            color = SystemPink
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
