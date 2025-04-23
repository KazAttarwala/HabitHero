package com.example.habithero.config
import com.example.habithero.BuildConfig

object ApiConfig {
    const val ANTHROPIC_API_KEY = BuildConfig.ANTHROPIC_API_KEY

    // This will be used in production to get the API key from Secret Manager
    //suspend fun getApiKey(): String = SecretManager.getAnthropicApiKey()
}