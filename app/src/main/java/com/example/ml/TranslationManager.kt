package com.example.ml

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class TranslationManager(private val context: Context) {

    private val modelManager = RemoteModelManager.getInstance()
    private val languageIdentifier = LanguageIdentification.getClient()

    private var arToEnTranslator: Translator? = null
    private var enToArTranslator: Translator? = null

    private val _modelDownloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val modelDownloadState: StateFlow<ModelDownloadState> = _modelDownloadState.asStateFlow()

    private val _isArabicModelDownloaded = MutableStateFlow(false)
    val isArabicModelDownloaded: StateFlow<Boolean> = _isArabicModelDownloaded.asStateFlow()

    private val _isEnglishModelDownloaded = MutableStateFlow(false)
    val isEnglishModelDownloaded: StateFlow<Boolean> = _isEnglishModelDownloaded.asStateFlow()

    suspend fun checkDownloadedModels() = withContext(Dispatchers.IO) {
        try {
            val models = awaitTask(modelManager.getDownloadedModels(TranslateRemoteModel::class.java))
            val languages = models.map { it.language }
            _isArabicModelDownloaded.value = languages.contains(TranslateLanguage.ARABIC)
            _isEnglishModelDownloaded.value = languages.contains(TranslateLanguage.ENGLISH)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking downloaded models", e)
        }
    }

    suspend fun downloadTranslationModels(): Boolean = withContext(Dispatchers.IO) {
        _modelDownloadState.value = ModelDownloadState.Downloading("جاري تنزيل نماذج الترجمة العربية والإنجليزية...")
        try {
            val conditions = DownloadConditions.Builder()
                .build()

            val arModel = TranslateRemoteModel.Builder(TranslateLanguage.ARABIC).build()
            val enModel = TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build()

            awaitTask(modelManager.download(arModel, conditions))
            _isArabicModelDownloaded.value = true

            awaitTask(modelManager.download(enModel, conditions))
            _isEnglishModelDownloaded.value = true

            _modelDownloadState.value = ModelDownloadState.Success("تم تنزيل نماذج الترجمة بنجاح وهي جاهزة للعمل دون إنترنت")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading translation models", e)
            _modelDownloadState.value = ModelDownloadState.Error("تعذر تنزيل النماذج: ${e.localizedMessage}")
            false
        }
    }

    suspend fun identifyLanguage(text: String): String = withContext(Dispatchers.IO) {
        try {
            val languageCode = awaitTask(languageIdentifier.identifyLanguage(text))
            if (languageCode == "und") {
                // If undetermined, check if it contains Arabic unicode characters
                if (containsArabic(text)) "ar" else "en"
            } else {
                languageCode
            }
        } catch (e: Exception) {
            if (containsArabic(text)) "ar" else "en"
        }
    }

    suspend fun translateText(text: String, forcedTargetLang: String? = null): TranslationResult = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext TranslationResult.Success("", "ar")

        try {
            val detectedLang = identifyLanguage(text)
            val sourceLang = if (detectedLang == "ar") TranslateLanguage.ARABIC else TranslateLanguage.ENGLISH
            val targetLang = if (forcedTargetLang != null) {
                if (forcedTargetLang == "ar") TranslateLanguage.ARABIC else TranslateLanguage.ENGLISH
            } else {
                if (sourceLang == TranslateLanguage.ARABIC) TranslateLanguage.ENGLISH else TranslateLanguage.ARABIC
            }

            val translator = getOrCreateTranslator(sourceLang, targetLang)
            // Ensure model is ready
            val conditions = DownloadConditions.Builder().build()
            awaitTask(translator.downloadModelIfNeeded(conditions))

            val translated = awaitTask(translator.translate(text))
            val targetLangCode = if (targetLang == TranslateLanguage.ARABIC) "ar" else "en"
            TranslationResult.Success(translated, targetLangCode)
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            TranslationResult.Error("فشلت الترجمة: ${e.localizedMessage}")
        }
    }

    private fun getOrCreateTranslator(source: String, target: String): Translator {
        return if (source == TranslateLanguage.ARABIC && target == TranslateLanguage.ENGLISH) {
            if (arToEnTranslator == null) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
                arToEnTranslator = Translation.getClient(options)
            }
            arToEnTranslator!!
        } else {
            if (enToArTranslator == null) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
                enToArTranslator = Translation.getClient(options)
            }
            enToArTranslator!!
        }
    }

    private fun containsArabic(text: String): Boolean {
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.ARABIC ||
                block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
                block == Character.UnicodeBlock.ARABIC_EXTENDED_A
            ) {
                return true
            }
        }
        return false
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            cont.resume(result)
        }.addOnFailureListener { exception ->
            cont.cancel(exception)
        }
    }

    fun close() {
        arToEnTranslator?.close()
        enToArTranslator?.close()
        languageIdentifier.close()
    }

    companion object {
        private const val TAG = "TranslationManager"
    }
}

sealed class TranslationResult {
    data class Success(val translatedText: String, val languageCode: String) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Downloading(val progressText: String) : ModelDownloadState()
    data class Success(val message: String) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}
