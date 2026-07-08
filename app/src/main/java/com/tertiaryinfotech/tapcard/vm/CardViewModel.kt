package com.tertiaryinfotech.tapcard.vm

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.tapcard.model.AnalyticsSummary
import com.tertiaryinfotech.tapcard.model.AppScreen
import com.tertiaryinfotech.tapcard.model.Contact
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.model.Lead
import com.tertiaryinfotech.tapcard.net.AuthResult
import com.tertiaryinfotech.tapcard.net.AuthStore
import com.tertiaryinfotech.tapcard.net.TapcardApi
import com.tertiaryinfotech.tapcard.ocr.CardScanner
import com.tertiaryinfotech.tapcard.util.CardParser
import com.tertiaryinfotech.tapcard.util.CardStore
import com.tertiaryinfotech.tapcard.util.OnboardingStore
import com.tertiaryinfotech.tapcard.widget.WidgetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A simple alert payload surfaced by the UI. */
data class AppAlert(val title: String, val message: String)

/**
 * Single source of truth for the app: holds the card list, the card being
 * edited/viewed, the active screen, auth state, and drives OCR + backend sync.
 *
 * When signed in, cards are backed by the shared web backend (two-way sync).
 * When signed out, the app works offline against local storage.
 */
class CardViewModel(app: Application) : AndroidViewModel(app) {

    private val store = CardStore(app)
    private val auth = AuthStore(app)
    private val onboardingStore = OnboardingStore(app)

    /** Whether the first-run onboarding should be shown (above auth/home). */
    var showOnboarding by mutableStateOf(!onboardingStore.seen)
        private set

    fun finishOnboarding() {
        onboardingStore.seen = true
        showOnboarding = false
    }

    var screen by mutableStateOf(AppScreen.HOME)
        private set

    var cards by mutableStateOf<List<DigitalCard>>(emptyList())
        private set

    /** The card being edited (REVIEW) or shown (CARD). */
    var draft by mutableStateOf(DigitalCard())

    /** Leads captured from the user's web cards (server-backed; empty offline). */
    var leads by mutableStateOf<List<Lead>>(emptyList())
        private set

    /** People the user has saved (server-backed; empty offline). */
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set

    var isPeopleLoading by mutableStateOf(false)
        private set

    /** Card-performance summary (server-backed; null until loaded). */
    var analytics by mutableStateOf<AnalyticsSummary?>(null)
        private set

    var isAnalyticsLoading by mutableStateOf(false)
        private set

    /** Which draft field an AI request is currently generating ("bio"/"about"), or null. */
    var generatingField by mutableStateOf<String?>(null)
        private set

    var isScanning by mutableStateOf(false)
        private set

    var isPublishing by mutableStateOf(false)
        private set

    /** True while a create/update sync request is in flight. */
    var isSaving by mutableStateOf(false)
        private set

    var publishedUrl by mutableStateOf<String?>(null)

    var activeAlert by mutableStateOf<AppAlert?>(null)

    /** The card currently shown in the share bottom sheet, or null if closed. */
    var shareCard by mutableStateOf<DigitalCard?>(null)

    // ---- Auth state ----

    var token by mutableStateOf(auth.token)
        private set
    var authUserName by mutableStateOf(auth.userName)
        private set
    var authUserEmail by mutableStateOf(auth.userEmail)
        private set

    /** Whether the login/sign-up screen is showing. Defaults on when signed out. */
    var showAuth by mutableStateOf(auth.token == null)
        private set
    var isAuthBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)

    val isAuthenticated: Boolean get() = token != null

    init {
        reload()
    }

    private fun reload() {
        val t = token
        if (t != null) refreshFromServer() else updateCards(store.allCards())
    }

    private fun refreshFromServer() {
        val t = token ?: return
        viewModelScope.launch {
            TapcardApi.fetchCards(t).onSuccess { updateCards(it) }
        }
    }

    /** Updates the card list and keeps the home-screen widget's card in sync. */
    private fun updateCards(list: List<DigitalCard>) {
        cards = list
        WidgetStore.savePrimary(getApplication(), list.firstOrNull())
    }

    /** Reloads the leads inbox + saved contacts from the backend. */
    fun refreshPeople() {
        val t = token ?: return
        isPeopleLoading = true
        viewModelScope.launch {
            TapcardApi.fetchLeads(t).onSuccess { leads = it }
            TapcardApi.fetchContacts(t).onSuccess { contacts = it }
            isPeopleLoading = false
        }
    }

    // ---- Navigation ----

    fun goHome() {
        reload()
        screen = AppScreen.HOME
    }

    fun startScan() {
        screen = AppScreen.SCAN
    }

    fun startManualEntry() {
        draft = DigitalCard()
        screen = AppScreen.REVIEW
    }

    fun openCard(card: DigitalCard) {
        draft = card
        screen = AppScreen.CARD
    }

    fun editDraft() {
        screen = AppScreen.REVIEW
    }

    fun openContacts() {
        refreshPeople()
        screen = AppScreen.CONTACTS
    }

    fun openAnalytics() {
        refreshAnalytics()
        screen = AppScreen.ANALYTICS
    }

    fun openSettings() {
        screen = AppScreen.SETTINGS
    }

    /** Opens the share bottom sheet for [card]. */
    fun openShare(card: DigitalCard) {
        shareCard = card
    }

    fun dismissShare() {
        shareCard = null
    }

    /** Reloads the card-performance summary from the backend. */
    fun refreshAnalytics() {
        val t = token ?: return
        isAnalyticsLoading = true
        viewModelScope.launch {
            TapcardApi.fetchAnalytics(t).onSuccess { analytics = it }
            isAnalyticsLoading = false
        }
    }

    // ---- People (leads + contacts) ----

    /** Saves a contact by hand, then refreshes the list. Requires sign-in. */
    fun addContact(contact: Contact) {
        val t = token ?: return
        if (contact.name.isBlank()) {
            activeAlert = AppAlert("Name needed", "Add a name before saving this contact.")
            return
        }
        viewModelScope.launch {
            TapcardApi.createContact(t, contact)
                .onSuccess { refreshPeople() }
                .onFailure { activeAlert = AppAlert("Couldn't save", it.message ?: "Please try again.") }
        }
    }

    fun deleteContact(contact: Contact) {
        val t = token ?: return
        viewModelScope.launch {
            TapcardApi.deleteContact(t, contact.id)
                .onSuccess { refreshPeople() }
                .onFailure { activeAlert = AppAlert("Couldn't delete", it.message ?: "Please try again.") }
        }
    }

    /** Converts a captured lead into a saved contact, then dismisses the lead. */
    fun saveLeadAsContact(lead: Lead) {
        val t = token ?: return
        viewModelScope.launch {
            TapcardApi.createContact(t, lead.toContact())
                .onSuccess {
                    TapcardApi.deleteLead(t, lead.id)
                    refreshPeople()
                }
                .onFailure { activeAlert = AppAlert("Couldn't save", it.message ?: "Please try again.") }
        }
    }

    fun dismissLead(lead: Lead) {
        val t = token ?: return
        viewModelScope.launch {
            TapcardApi.deleteLead(t, lead.id)
                .onSuccess { refreshPeople() }
                .onFailure { activeAlert = AppAlert("Couldn't dismiss", it.message ?: "Please try again.") }
        }
    }

    // ---- Auth actions ----

    fun openAuth() {
        authError = null
        showAuth = true
    }

    /** Dismiss the auth screen and use the app offline (local storage only). */
    fun continueOffline() {
        showAuth = false
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            authError = "Enter your email and password."
            return
        }
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.login(email, password)
                .onSuccess { onAuthSuccess(it) }
                .onFailure {
                    authError = it.message ?: "Login failed."
                    isAuthBusy = false
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.length < 8) {
            authError = "Enter a name, email, and a password of at least 8 characters."
            return
        }
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.register(name, email, password)
                .onSuccess { onAuthSuccess(it) }
                .onFailure {
                    authError = it.message ?: "Sign-up failed."
                    isAuthBusy = false
                }
        }
    }

    private fun onAuthSuccess(result: AuthResult) {
        auth.save(result.token, result.name, result.email)
        token = result.token
        authUserName = result.name
        authUserEmail = result.email
        isAuthBusy = false
        authError = null
        showAuth = false
        screen = AppScreen.HOME
        refreshFromServer()
    }

    fun logout() {
        auth.clear()
        token = null
        authUserName = null
        authUserEmail = null
        updateCards(store.allCards())
        leads = emptyList()
        contacts = emptyList()
        analytics = null
        screen = AppScreen.HOME
    }

    // ---- AI text generation ----

    /**
     * Drafts the given [field] ("bio" or "about") from the current name/title/company
     * via the backend AI, writing the result straight into the draft. Requires sign-in.
     */
    fun generateField(field: String) {
        val t = token
        if (t == null) {
            activeAlert = AppAlert(
                "Sign in for AI",
                "Writing with AI needs a Tapcard account. Sign in, then try again.",
            )
            return
        }
        if (draft.name.isBlank()) {
            activeAlert = AppAlert("Add a name first", "The AI needs at least a name to write from.")
            return
        }
        if (generatingField != null) return
        generatingField = field
        viewModelScope.launch {
            val result = TapcardApi.generateText(t, field, draft.name, draft.title, draft.company)
            generatingField = null
            result.onSuccess { text ->
                draft = when (field) {
                    "about" -> draft.copy(about = text)
                    else -> draft.copy(bio = text)
                }
            }.onFailure {
                activeAlert = AppAlert("AI couldn't write that", it.message ?: "Please try again.")
            }
        }
    }

    // ---- Scanning ----

    fun onImageCaptured(uri: Uri) {
        isScanning = true
        viewModelScope.launch {
            val result = runCatching {
                // Universal scan: try a QR/barcode first (vCard/MECARD/profile link),
                // then fall back to reading the printed text (OCR).
                CardScanner.scanCardFromQr(getApplication(), uri)
                    ?: CardParser.parse(
                        withContext(Dispatchers.Default) { CardScanner.recognize(getApplication(), uri) },
                    )
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

    // ---- Persistence (local or synced) ----

    fun saveDraft(now: Long) {
        if (draft.isBlank) {
            activeAlert = AppAlert("Nothing to save", "Add at least a name, phone or email first.")
            return
        }

        val t = token
        if (t == null) {
            // Offline: save locally (existing behavior).
            val toSave = if (draft.createdAtEpoch == 0L) draft.copy(createdAtEpoch = now) else draft
            draft = toSave
            store.save(toSave)
            reload()
            screen = AppScreen.CARD
            return
        }

        // Signed in: sync to the backend. A name is required server-side.
        if (draft.name.isBlank()) {
            activeAlert = AppAlert("Name needed", "Add a name before saving this card.")
            return
        }
        if (isSaving) return
        isSaving = true
        val isNew = draft.createdAtEpoch == 0L
        viewModelScope.launch {
            val result = if (isNew) TapcardApi.createCard(t, draft) else TapcardApi.updateCard(t, draft)
            isSaving = false
            result.onSuccess { saved ->
                draft = saved
                refreshFromServer()
                screen = AppScreen.CARD
            }.onFailure {
                activeAlert = AppAlert(
                    "Couldn't save",
                    it.message ?: "Please check your connection and try again.",
                )
            }
        }
    }

    fun deleteCard(card: DigitalCard) {
        val t = token
        if (t == null) {
            store.delete(card.id)
            reload()
            if (draft.id == card.id) screen = AppScreen.HOME
            return
        }
        viewModelScope.launch {
            TapcardApi.deleteCard(t, card.id)
                .onSuccess {
                    refreshFromServer()
                    if (draft.id == card.id) screen = AppScreen.HOME
                }
                .onFailure {
                    activeAlert = AppAlert("Couldn't delete", it.message ?: "Please try again.")
                }
        }
    }

    // ---- Web publishing (onboard) ----

    fun publishDraft() {
        val card = draft
        if (card.name.isBlank()) {
            activeAlert = AppAlert("Name needed", "Add a name before publishing this card to the web.")
            return
        }
        if (card.email.isBlank()) {
            activeAlert = AppAlert(
                "Email needed",
                "Publishing creates a live web card and an account, which needs a valid email address. Add one to the card first.",
            )
            return
        }
        if (isPublishing) return

        isPublishing = true
        viewModelScope.launch {
            val result = TapcardApi.publish(card)
            isPublishing = false
            result
                .onSuccess { publishedUrl = it.url }
                .onFailure {
                    activeAlert = AppAlert(
                        "Couldn't publish",
                        it.message ?: "Please check your internet connection and try again.",
                    )
                }
        }
    }

    fun dismissPublished() {
        publishedUrl = null
    }
}