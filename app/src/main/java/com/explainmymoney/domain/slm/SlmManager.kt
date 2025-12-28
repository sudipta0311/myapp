package com.explainmymoney.domain.slm

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class DeviceCapability(
    val totalRamGb: Double,
    val availableRamGb: Double,
    val availableStorageGb: Double,
    val isCapable: Boolean,
    val reason: String?
)

sealed class SlmDownloadState {
    object Idle : SlmDownloadState()
    data class Downloading(val progress: Float) : SlmDownloadState()
    object Completed : SlmDownloadState()
    data class Error(val message: String) : SlmDownloadState()
}

class SlmManager(private val context: Context) {
    
    companion object {
        const val MIN_RAM_GB = 4.0
        const val MIN_STORAGE_GB = 3.0
        const val MODEL_SIZE_GB = 2.0
        const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"
        const val MODEL_DIR = "slm_models"
    }
    
    private val _downloadState = MutableStateFlow<SlmDownloadState>(SlmDownloadState.Idle)
    val downloadState: StateFlow<SlmDownloadState> = _downloadState.asStateFlow()
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()
    
    private var slmInference: SlmInference? = null
    
    fun checkDeviceCapability(): DeviceCapability {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availableRamGb = memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        
        val modelDir = File(context.filesDir, MODEL_DIR)
        val availableStorageGb = context.filesDir.usableSpace / (1024.0 * 1024.0 * 1024.0)
        
        val ramSufficient = totalRamGb >= MIN_RAM_GB
        val storageSufficient = availableStorageGb >= MIN_STORAGE_GB
        val apiLevelSufficient = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        
        val isCapable = ramSufficient && storageSufficient && apiLevelSufficient
        
        val reason = when {
            !ramSufficient -> "Device needs at least ${MIN_RAM_GB}GB RAM (has ${String.format("%.1f", totalRamGb)}GB)"
            !storageSufficient -> "Not enough storage space (needs ${MIN_STORAGE_GB}GB, has ${String.format("%.1f", availableStorageGb)}GB)"
            !apiLevelSufficient -> "Requires Android 8.0 or higher"
            else -> null
        }
        
        return DeviceCapability(
            totalRamGb = totalRamGb,
            availableRamGb = availableRamGb,
            availableStorageGb = availableStorageGb,
            isCapable = isCapable,
            reason = reason
        )
    }
    
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, "$MODEL_DIR/$MODEL_FILENAME")
        return modelFile.exists() && modelFile.length() > 0
    }
    
    fun getModelPath(): String? {
        val modelFile = File(context.filesDir, "$MODEL_DIR/$MODEL_FILENAME")
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    suspend fun downloadModel(onProgress: (Float) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = SlmDownloadState.Downloading(0f)
            
            val modelDir = File(context.filesDir, MODEL_DIR)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, MODEL_FILENAME)
            
            simulateModelDownload(modelFile, onProgress)
            
            _downloadState.value = SlmDownloadState.Completed
            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            _downloadState.value = SlmDownloadState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }
    
    private suspend fun simulateModelDownload(targetFile: File, onProgress: (Float) -> Unit) {
        for (i in 0..100 step 5) {
            kotlinx.coroutines.delay(100)
            val progress = i / 100f
            onProgress(progress)
            _downloadState.value = SlmDownloadState.Downloading(progress)
        }
        
        targetFile.writeText("SIMULATED_MODEL_PLACEHOLDER")
    }
    
    suspend fun initializeModel(): Result<SlmInference> = withContext(Dispatchers.IO) {
        try {
            val modelPath = getModelPath()
            if (modelPath == null) {
                return@withContext Result.failure(IllegalStateException("Model not downloaded"))
            }
            
            val inference = SlmInference(context, modelPath)
            inference.initialize()
            slmInference = inference
            _isModelReady.value = true
            
            Result.success(inference)
        } catch (e: Exception) {
            _isModelReady.value = false
            Result.failure(e)
        }
    }
    
    fun getInference(): SlmInference? = slmInference
    
    suspend fun deleteModel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            slmInference?.close()
            slmInference = null
            _isModelReady.value = false
            
            val modelFile = File(context.filesDir, "$MODEL_DIR/$MODEL_FILENAME")
            if (modelFile.exists()) {
                modelFile.delete()
            }
            
            _downloadState.value = SlmDownloadState.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun close() {
        slmInference?.close()
        slmInference = null
        _isModelReady.value = false
    }
}
