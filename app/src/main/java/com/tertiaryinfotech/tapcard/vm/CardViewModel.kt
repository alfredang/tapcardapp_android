package com.tertiaryinfotech.tapcard.vm

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.tapcard.model.AppScreen
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.ocr.CardScanner
import com.tertiaryinfotech.tapcard.util.CardParser
import com.tertiaryinfotech.tapcard.util.CardStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A simple alert payload surfaced by the UI. */
data class AppAlert(val title: String, val message: String)

/**
 * Single source of truth for the app: holds the saved-card list, the card
 * currently being edited/viewed, the active screen, and drives OCR scanning.
 * Mirrors the iOS app's view-model layout.
 */
class CardViewModel(app: Application) : AndroidViewModel(app) {

    private val store = CardStore(app)

    var screen by mutableStateOf(AppScreen.HOME)
        private set

    var cards by mutableStateOf<List<DigitalCard>>(emptyList())
        private set

    /** The card being edited (REVIEW) or shown (CARD). */
    var draft by mutableStateOf(DigitalCard())

    var isScanning by mutableStateOf(false)
        private set

    var activeAlert by mutableStateOf<AppAlert?>(null)

    init {
        reload()
    }

    private fun reload() {
        cards = store.allCards()
    }

    // ---- Navigation ----

    fun goHome() {
        reload()
        screen = AppScreen.HOME
    }

    fun startScan() {
        screen = AppScreen.SCAN
    }

    /** Begin a card from scratch with no scan. */
    fun startManualEntry() {
        draft = DigitalCard()
        screen = AppScreen.REVIEW
    }

    /** Open an existing saved card. */
    fun openCard(card: DigitalCard) {
        draft = card
        screen = AppScreen.CARD
    }

    /** Edit the card currently being viewed. */
    fun editDraft() {
        screen = AppScreen.REVIEW
    }

    // ---- Scanning ----

    /**
     * Runs OCR on the captured image, parses it into fields, and moves to REVIEW.
     * Failures fall back to a blank manual-entry form with an alert.
     */
    fun onImageCaptured(uri: Uri) {
        isScanning = true
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.Default) {
                    CardScanner.recognize(getApplication(), uri)
                }
                CardParser.parse(text)
            }
            isScanning = false
            result.onSuccess { parsed ->
                draft = if (parsed.isBlank) {
                    activeAlert = AppAlert(
                        "No text found",
                        "Couldn't read any details from that photo. Try again with better lighting, or fill the card in by hand.",
                    )
                    DigitalCard()
                } else {
                    parsed
                }
                screen = AppScreen.REVIEW
            }.onFailure {
                activeAlert = AppAlert("Scan failed", it.message ?: "Could not process the image.")
                draft = DigitalCard()
                screen = AppScreen.REVIEW
            }
        }
    }

    // ---- Persistence ----

    /** Saves the current draft (stamping a creation time on first save) and shows it. */
    fun saveDraft(now: Long) {
        if (draft.isBlank) {
            activeAlert = AppAlert("Nothing to save", "Add at least a name, phone or email first.")
            return
        }
        val toSave = if (draft.createdAtEpoch == 0L) draft.copy(createdAtEpoch = now) else draft
        draft = toSave
        store.save(toSave)
        reload()
        screen = AppScreen.CARD
    }

    fun deleteCard(card: DigitalCard) {
        store.delete(card.id)
        reload()
        if (draft.id == card.id) screen = AppScreen.HOME
    }
}
