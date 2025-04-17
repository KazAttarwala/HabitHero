package com.example.habithero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habithero.api.AnthropicService
import com.example.habithero.api.QuoteResponse
import com.example.habithero.config.ApiConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

class QuoteViewModel : ViewModel() {
    private val anthropicService = AnthropicService()
    
    // Get API key from config
    private val apiKey = ApiConfig.ANTHROPIC_API_KEY
    
    private val _quote = MutableLiveData<QuoteResponse>()
    val quote: LiveData<QuoteResponse> = _quote
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var quoteTimerJob: Job? = null
    
    fun fetchQuote() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val quoteResponse = anthropicService.getMotivationalQuote(apiKey)
                _quote.value = quoteResponse
                
            } catch (e: Exception) {
                Log.e("QuoteViewModel", "Error fetching quote", e)
                _error.value = "Failed to load quote: ${e.message}"
                
                // Provide a fallback quote if the API call fails
                _quote.value = QuoteResponse(
                    quote = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
                    author = "Aristotle"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startAutoRefreshQuotes(intervalMs: Long = 15000) {
        // Cancel any existing job first
        stopAutoRefreshQuotes()
        
        // Start a new job to fetch quotes periodically with retry logic
        quoteTimerJob = viewModelScope.launch {
            var currentRetry = 0
            val maxRetries = 3
            
            while (isActive) {
                try {
                    fetchQuote()
                    // Reset retry counter on success
                    currentRetry = 0
                    delay(intervalMs)
                } catch (e: Exception) {
                    Log.e("QuoteViewModel", "Auto refresh failed, attempt: ${currentRetry + 1}", e)
                    // Implement exponential backoff
                    currentRetry++
                    if (currentRetry <= maxRetries) {
                        // Use exponential backoff: intervalMs * 2^retryCount, capped at 60 seconds
                        val backoffDelay = (intervalMs * 2.0.pow(currentRetry.toDouble())).toLong().coerceAtMost(60000)
                        Log.d("QuoteViewModel", "Backing off for $backoffDelay ms before retry")
                        delay(backoffDelay)
                    } else {
                        // After max retries, just go back to regular interval
                        currentRetry = 0
                        delay(intervalMs)
                    }
                }
            }
        }
    }

    fun stopAutoRefreshQuotes() {
        quoteTimerJob?.cancel()
        quoteTimerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefreshQuotes()
    }
}