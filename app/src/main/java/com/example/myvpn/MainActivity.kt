package com.example.myvpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: VpnViewModel

    private lateinit var spinnerProtocol: Spinner
    private lateinit var etServer: EditText
    private lateinit var etPort: EditText
    private lateinit var etPayload: EditText
    private lateinit var etSni: EditText
    private lateinit var btnToggleVpn: Button
    private lateinit var btnExportConfig: Button
    private lateinit var btnImportConfig: Button

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "Permission VPN Refused", Toast.LENGTH_SHORT).show()
        }
    }

    private val importConfigLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                val json = reader.readText()
                val imported = AppConfig.fromJson(json)
                if (imported != null) {
                    applyConfigToUi(imported)
                    Toast.makeText(this, "Config Imported Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid Config File", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val exportConfigLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                val currentConfig = getConfigFromUi()
                writer.write(currentConfig.toJson())
                Toast.makeText(this, "Config Exported Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[VpnViewModel::class.java]

        spinnerProtocol = findViewById(R.id.spinnerProtocol)
        etServer = findViewById(R.id.etServer)
        etPort = findViewById(R.id.etPort)
        etPayload = findViewById(R.id.etPayload)
        etSni = findViewById(R.id.etSni)
        btnToggleVpn = findViewById(R.id.btnToggleVpn)
        btnExportConfig = findViewById(R.id.btnExportConfig)
        btnImportConfig = findViewById(R.id.btnImportConfig)

        setupProtocolSpinner()

        viewModel.vpnStatus.observe(this) { isConnected ->
            btnToggleVpn.text = if (isConnected) "STOP VPN" else "START VPN"
        }

        btnToggleVpn.setOnClickListener {
            if (viewModel.vpnStatus.value == true) {
                stopVpnService()
            } else {
                prepareAndStartVpn()
            }
        }

        btnExportConfig.setOnClickListener {
            exportConfigLauncher.launch("myvpn_config.json")
        }

        btnImportConfig.setOnClickListener {
            importConfigLauncher.launch("*/*")
        }
    }

    private fun setupProtocolSpinner() {
        val protocols = VpnProtocol.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, protocols)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProtocol.adapter = adapter
    }

    private fun getConfigFromUi(): AppConfig {
        val selectedProto = VpnProtocol.valueOf(spinnerProtocol.selectedItem.toString())
        return AppConfig(
            protocol = selectedProto,
            serverAddress = etServer.text.toString(),
            serverPort = etPort.text.toString().toIntOrNull() ?: 443,
            payload = etPayload.text.toString(),
            sni = etSni.text.toString()
        )
    }

    private fun applyConfigToUi(config: AppConfig) {
        spinnerProtocol.setSelection(config.protocol.ordinal)
        etServer.setText(config.serverAddress)
        etPort.setText(config.serverPort.toString())
        etPayload.setText(config.payload)
        etSni.setText(config.sni)
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val config = getConfigFromUi()
        val intent = Intent(this, VpnTun::class.java).apply {
            putExtra("protocol", config.protocol.name)
            putExtra("serverAddress", config.serverAddress)
            putExtra("serverPort", config.serverPort)
            putExtra("payload", config.payload)
            putExtra("sni", config.sni)
        }
        startService(intent)
        viewModel.setConnected(true)
    }

    private fun stopVpnService() {
        val intent = Intent(this, VpnTun::class.java).apply {
            action = "STOP"
        }
        startService(intent)
        viewModel.setConnected(false)
    }
}