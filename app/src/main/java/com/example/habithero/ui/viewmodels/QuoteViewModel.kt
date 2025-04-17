package com.example.habithero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habithero.api.AnthropicService
import com.example.habithero.api.QuoteResponse
import com.example.habithero.config.ApiConfig
import kotlinx.coroutines.launch

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
} 