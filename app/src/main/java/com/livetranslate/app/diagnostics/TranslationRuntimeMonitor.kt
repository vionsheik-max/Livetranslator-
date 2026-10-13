package com.livetranslate.app.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface TranslationRuntimeState {
    data object Idle : TranslationRuntimeState
    data object Starting : TranslationRuntimeState
    data class Ready(val visibleLines: Int) : TranslationRuntimeState
    data object WaitingForText : TranslationRuntimeState
    data class Error(val message: String) : TranslationRuntimeState
}

class TranslationRuntimeMonitor {
    private val mutableState = MutableStateFlow<TranslationRuntimeState>(TranslationRuntimeState.Idle)
    val state: StateFlow<TranslationRuntimeState> = mutableState

    fun update(next: TranslationRuntimeState) {
        mutableState.value = next
    }
}
