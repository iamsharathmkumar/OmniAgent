package com.omniagent.assistant.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.omniagent.assistant.service.AgentAccessibilityService
import com.omniagent.assistant.service.FloatingOverlayService
import com.omniagent.assistant.service.VoiceAssistantService
import com.omniagent.assistant.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission required for voice commands", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmniAgentAppTheme {
                MainDashboardScreen(
                    onRequestAudioPermission = {
                        requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }
}

@Composable
fun OmniAgentAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6366F1),
            secondary = Color(0xFF10B981),
            background = Color(0xFF090D16),
            surface = Color(0xFF131B2E),
            onPrimary = Color.White,
            onSurface = Color(0xFFE2E8F0)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(onRequestAudioPermission: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE) }

    var accessibilityEnabled by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var overlayEnabled by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var audioPermissionGranted by remember { mutableStateOf(PermissionHelper.hasAudioPermission(context)) }

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var selectedProvider by remember { mutableStateOf(prefs.getString("provider", "OPENAI") ?: "OPENAI") }
    var selectedModel by remember { mutableStateOf(prefs.getString("model", "gpt-4o") ?: "gpt-4o") }
    var customCommandText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (AgentAccessibilityService.isServiceRunning) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OmniAgent AI", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1120),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF090D16)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Autonomous OS Agent",
                        color = Color(0xFF818CF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Controls any app via Voice",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "OmniAgent inspects on-screen elements via Android Accessibility, plans multi-hop actions using Multimodal LLMs, and executes gestures and typing automatically.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(context, FloatingOverlayService::class.java)
                                context.startService(intent)
                                Toast.makeText(context, "Floating HUD activated", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Floating HUD")
                        }
                    }
                }
            }

            // Permissions Checklist
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SYSTEM CAPABILITIES", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    PermissionRow(
                        title = "1. Accessibility Service",
                        desc = "Needed to observe UI tree & execute clicks",
                        isGranted = AgentAccessibilityService.isServiceRunning,
                        onEnable = { PermissionHelper.openAccessibilitySettings(context) }
                    )

                    Divider(color = Color(0xFF1E293B))

                    PermissionRow(
                        title = "2. Draw Over Other Apps",
                        desc = "Needed for floating Dynamic Island & HUD",
                        isGranted = PermissionHelper.canDrawOverlays(context),
                        onEnable = { PermissionHelper.openOverlaySettings(context) }
                    )

                    Divider(color = Color(0xFF1E293B))

                    PermissionRow(
                        title = "3. Microphone Permission",
                        desc = "Continuous voice listening & speech recognition",
                        isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                        onEnable = onRequestAudioPermission
                    )
                }
            }

            // LLM Configuration Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AI REASONING BRAIN", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    // Provider Tabs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OPENAI", "GEMINI", "ANTHROPIC").forEach { prov ->
                            FilterChip(
                                selected = selectedProvider == prov,
                                onClick = {
                                    selectedProvider = prov
                                    selectedModel = when (prov) {
                                        "OPENAI" -> "gpt-4o"
                                        "GEMINI" -> "gemini-2.0-flash"
                                        "ANTHROPIC" -> "claude-3-5-sonnet-20241022"
                                        else -> "gpt-4o"
                                    }
                                    prefs.edit().putString("provider", prov).putString("model", selectedModel).apply()
                                },
                                label = { Text(prov, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutEmptyField(
                        label = "Model",
                        value = selectedModel,
                        onValueChange = {
                            selectedModel = it
                            prefs.edit().putString("model", it).apply()
                        }
                    )

                    OutEmptyField(
                        label = "API Key (Leave empty to use built-in simulator)",
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.edit().putString("api_key", it).apply()
                        }
                    )
                }
            }

            // Quick Voice & Task Tester
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("QUICK TEST VOICE COMMANDS", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    val sampleCommands = listOf(
                        "Send a message to Alex on WhatsApp saying I'll be 10m late",
                        "Order a Margherita pizza on DoorDash",
                        "Set an alarm for 7:00 AM tomorrow",
                        "Open Settings and turn on Dark Mode"
                    )

                    sampleCommands.forEach { cmd ->
                        OutlinedButton(
                            onClick = {
                                triggerTask(context, cmd)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🗣 \"$cmd\"", fontSize = 12.sp, color = Color(0xFFC7D2FE))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customCommandText,
                            onValueChange = { customCommandText = it },
                            placeholder = { Text("Or type any custom command...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (customCommandText.isNotBlank()) {
                                    triggerTask(context, customCommandText)
                                    customCommandText = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Run", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun triggerTask(context: Context, text: String) {
    val intent = Intent(context, VoiceAssistantService::class.java).apply {
        action = VoiceAssistantService.ACTION_EXECUTE_TEXT
        putExtra(VoiceAssistantService.EXTRA_TEXT_COMMAND, text)
    }
    context.startService(intent)
    Toast.makeText(context, "Executing: $text", Toast.LENGTH_SHORT).show()
}

@Composable
fun PermissionRow(title: String, desc: String, isGranted: Boolean, onEnable: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(desc, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        if (isGranted) {
            Surface(
                color = Color(0xFF064E3B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ENABLED", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        } else {
            Button(
                onClick = onEnable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enable", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun OutEmptyField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
