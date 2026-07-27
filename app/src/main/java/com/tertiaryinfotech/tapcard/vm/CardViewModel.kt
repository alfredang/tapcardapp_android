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
import com.tertiaryinfotech.tapcard.model.Appointment
import com.tertiaryinfotech.tapcard.model.Contact
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.model.Lead
import com.tertiaryinfotech.tapcard.model.Task
import com.tertiaryinfotech.tapcard.net.AuthResult
import com.tertiaryinfotech.tapcard.net.AuthStore
import com.tertiaryinfotech.tapcard.net.TapcardApi
import com.tertiaryinfotech.tapcard.ocr.CardScanner
import com.tertiaryinfotech.tapcard.util.CardParser
import com.tertiaryinfotech.tapcard.util.CardStore
import com.tertiaryinfotech.tapcard.widget.WidgetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A simple alert payload surfaced by the UI. */
data class AppAlert(val title: String, val message: String)

/** Which step of the passwordless email-OTP flow the auth screen is on. */
enum class AuthStep { EMAIL, CODE }

/** Why the scanner was opened: to digitize YOUR paper card, or to save a person you met. */
enum class ScanIntent { MY_CARD, CONTACT }

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

    /** Planner: the signed-in user's tasks and appointments (server-backed). */
    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var appointments by mutableStateOf<List<Appointment>>(emptyList())
        private set
    var isPlannerLoading by mutableStateOf(false)
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

    /**
     * While on the auth gate, whether to show the welcome landing page (vs the
     * actual sign-in / sign-up form). Blinq-style: land → choose Get started / Log in.
     */
    var authLanding by mutableStateOf(true)
        private set

    /** Whether the flow was entered via "Log in" (vs "Get started") — copy only. */
    var authStartInLogin by mutableStateOf(false)
        private set

    /** Current step of the passwordless flow: enter email → enter code. */
    var authStep by mutableStateOf(AuthStep.EMAIL)
        private set

    /** The email a code was sent to (carried into the verify step). */
    var authEmail by mutableStateOf("")
        private set

    var isAuthBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)

    val isAuthenticated: Boolean get() = token != null

    /**
     * True while loading into the authenticated app — the "Hang tight!" splash is
     * shown while cards are fetched (on cold start when already signed in, and
     * right after a successful login). Logged-out users go straight to the welcome
     * page and never see it.
     */
    var isBooting by mutableStateOf(false)
        private set

    init {
        val t = token
        if (t != null) enterAppWithSplash(t) else updateCards(store.allCards())
    }

    /** Show the splash while the initial card fetch validates the session. */
    private fun enterAppWithSplash(token: String) {
        isBooting = true
        viewModelScope.launch {
            TapcardApi.fetchCards(token)
                .onSuccess { updateCards(it) }
                .onFailure { updateCards(store.allCards()) }
            // Floor the splash so a fast fetch doesn't flash the mark on/off.
            delay(BOOT_MIN_MS)
            isBooting = false
        }
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

    /** Why the current scan was started — drives where a scanned card lands. */
    var scanIntent by mutableStateOf(ScanIntent.CONTACT)
        private set

    /** Scan tab: capture someone you met → save to Contacts. */
    fun startScan() {
        scanIntent = ScanIntent.CONTACT
        screen = AppScreen.SCAN
    }

    /** "Create your first card": scan your own paper card to prefill YOUR card. */
    fun scanForMyCard() {
        scanIntent = ScanIntent.MY_CARD
        screen = AppScreen.SCAN
    }

    /** Where a scanned card should land, based on why the scanner was opened. */
    private fun scanDestination(): AppScreen =
        if (scanIntent == ScanIntent.MY_CARD) AppScreen.REVIEW else AppScreen.CONTACT_REVIEW

    fun startManualEntry() {
        draft = DigitalCard()
        screen = AppScreen.REVIEW
    }

    /**
     * True when the contact-review screen was opened for MANUAL entry (blank,
     * editable) rather than from a scan (populated, read-only).
     */
    var contactManualEntry by mutableStateOf(false)
        private set

    /**
     * "Enter manually" from the scanner. Manual entry needs an editable form:
     *  • MY_CARD  → the card editor (build your own card)
     *  • CONTACT  → the contact-review screen in editable mode
     * (Scanned data, by contrast, lands on the same screen read-only.)
     */
    fun enterScannedManually() {
        draft = DigitalCard()
        if (scanIntent == ScanIntent.MY_CARD) {
            screen = AppScreen.REVIEW
        } else {
            contactManualEntry = true
            screen = AppScreen.CONTACT_REVIEW
        }
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

    // ---- Planner (tasks + appointments) ----

    fun openPlanner() {
        refreshPlanner()
        screen = AppScreen.PLANNER
    }

    fun refreshPlanner() {
        val t = token ?: return
        isPlannerLoading = true
        viewModelScope.launch {
            TapcardApi.fetchTasks(t).onSuccess { tasks = it }
            TapcardApi.fetchAppointments(t).onSuccess { appointments = it }
            isPlannerLoading = false
        }
    }

    fun addTask(title: String, type: String, dueAt: String?) {
        val t = token ?: return
        if (title.isBlank()) {
            activeAlert = AppAlert("Title needed", "Give the task a title first.")
            return
        }
        viewModelScope.launch {
            TapcardApi.createTask(t, title, type, dueAt)
                .onSuccess { refreshPlanner() }
                .onFailure { activeAlert = AppAlert("Couldn't add task", it.message ?: "Please try again.") }
        }
    }

    /** Edit a task's title / type / due date. */
    fun editTask(task: Task, title: String, type: String, dueAt: String?) {
        val t = token ?: return
        if (title.isBlank()) {
            activeAlert = AppAlert("Title needed", "Give the task a title first.")
            return
        }
        viewModelScope.launch {
            TapcardApi.updateTask(t, task.id, title, type, dueAt)
                .onSuccess { refreshPlanner() }
                .onFailure { activeAlert = AppAlert("Couldn't update task", it.message ?: "Please try again.") }
        }
    }

    /** Optimistically flip a task between DONE and TODO. */
    fun toggleTask(task: Task) {
        val t = token ?: return
        val next = if (task.isDone) "TODO" else "DONE"
        tasks = tasks.map { if (it.id == task.id) it.copy(status = next) else it }
        viewModelScope.launch {
            TapcardApi.setTaskStatus(t, task.id, next).onFailure { refreshPlanner() }
        }
    }

    fun removeTask(task: Task) {
        val t = token ?: return
        tasks = tasks.filterNot { it.id == task.id }
        viewModelScope.launch {
            TapcardApi.deleteTask(t, task.id).onFailure { refreshPlanner() }
        }
    }

    fun addAppointment(name: String, email: String?, startAt: String, endAt: String, notes: String?) {
        val t = token ?: return
        if (name.isBlank()) {
            activeAlert = AppAlert("Name needed", "Add a name for this appointment.")
            return
        }
        viewModelScope.launch {
            TapcardApi.createAppointment(t, name, email, startAt, endAt, notes)
                .onSuccess { refreshPlanner() }
                .onFailure { activeAlert = AppAlert("Couldn't add", it.message ?: "Please try again.") }
        }
    }

    fun editAppointment(appt: Appointment, name: String, email: String?, startAt: String, endAt: String, notes: String?) {
        val t = token ?: return
        if (name.isBlank()) {
            activeAlert = AppAlert("Name needed", "Add a name for this appointment.")
            return
        }
        viewModelScope.launch {
            TapcardApi.updateAppointment(t, appt.id, name, email, startAt, endAt, notes)
                .onSuccess { refreshPlanner() }
                .onFailure { activeAlert = AppAlert("Couldn't save", it.message ?: "Please try again.") }
        }
    }

    fun removeAppointment(appt: Appointment) {
        val t = token ?: return
        appointments = appointments.filterNot { it.id == appt.id }
        viewModelScope.launch {
            TapcardApi.deleteAppointment(t, appt.id).onFailure { refreshPlanner() }
        }
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
        authLanding = true
        showAuth = true
    }

    /** From the welcome landing → open the email step (sign-up framing). */
    fun beginSignUp() {
        resetOtpFlow()
        authStartInLogin = false
        authLanding = false
    }

    /** From the welcome landing → open the email step (login framing). */
    fun beginLogin() {
        resetOtpFlow()
        authStartInLogin = true
        authLanding = false
    }

    /** From the form's back arrow → return to the welcome landing. */
    fun backToWelcome() {
        authError = null
        authLanding = true
    }

    private fun resetOtpFlow() {
        authStep = AuthStep.EMAIL
        authEmail = ""
        authError = null
        isAuthBusy = false
    }

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** Step 1 — email a 6-digit code, then advance to the code step. */
    fun requestOtp(email: String) {
        val trimmed = email.trim()
        if (!emailRegex.matches(trimmed)) {
            authError = "Enter a valid email address."
            return
        }
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.requestOtp(trimmed)
                .onSuccess {
                    authEmail = trimmed
                    authStep = AuthStep.CODE
                    isAuthBusy = false
                }
                .onFailure {
                    authError = it.message ?: "Couldn't send the code. Try again."
                    isAuthBusy = false
                }
        }
    }

    /** Step 2 — verify the code (and name for new accounts) → sign in. */
    fun verifyOtp(code: String, name: String? = null) {
        if (code.trim().length < 4) {
            authError = "Enter the code from your email."
            return
        }
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.verifyOtp(authEmail, code.trim(), name?.takeIf { it.isNotBlank() })
                .onSuccess { onAuthSuccess(it) }
                .onFailure {
                    authError = it.message ?: "That code didn't work."
                    isAuthBusy = false
                }
        }
    }

    /** Re-send a fresh code to the same email. */
    fun resendOtp() {
        if (authEmail.isNotBlank()) requestOtp(authEmail)
    }

    /** Back arrow on the code step → return to the email step. */
    fun backToEmailStep() {
        authStep = AuthStep.EMAIL
        authError = null
        isAuthBusy = false
    }

    // ---- Google sign-in ----

    /**
     * Completes sign-in with a Google ID token from the system account picker
     * (see [com.tertiaryinfotech.tapcard.net.requestGoogleIdToken]). Same flow as
     * OTP/password: a new Google account is created server-side, an existing one
     * signs straight in.
     */
    fun signInWithGoogle(idToken: String) {
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.googleSignIn(idToken, null)
                .onSuccess { onAuthSuccess(it) }
                .onFailure {
                    authError = it.message ?: "Google sign-in failed. Try again."
                    isAuthBusy = false
                }
        }
    }

    // ---- Password auth ----

    /** Create a new account with name + email + password. */
    fun signUpWithPassword(name: String, email: String, password: String) {
        val e = email.trim()
        if (name.isBlank()) { authError = "Enter your name."; return }
        if (!emailRegex.matches(e)) { authError = "Enter a valid email address."; return }
        if (password.length < 6) { authError = "Use a password of at least 6 characters."; return }
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.register(name.trim(), e, password)
                .onSuccess { onAuthSuccess(it) }
                .onFailure {
                    authError = it.message ?: "Couldn't create your account. Try again."
                    isAuthBusy = false
                }
        }
    }

    /** Log in with email + password. */
    fun logInWithPassword(email: String, password: String) {
        val e = email.trim()
        if (!emailRegex.matches(e)) { authError = "Enter a valid email address."; return }
        if (password.isBlank()) { authError = "Enter your password."; return }
        isAuthBusy = true
        authError = null
        viewModelScope.launch {
            TapcardApi.login(e, password)
                .onSuccess { onAuthSuccess(it) }
                .onFailure {
                    authError = it.message ?: "Invalid email or password."
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
        // Show "Hang tight!" while the freshly signed-in account's cards load.
        enterAppWithSplash(result.token)
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
        // Return to the welcome landing page, not the offline home.
        authLanding = true
        showAuth = true
    }

    /** Permanently deletes the signed-in account, then drops back to the offline home. */
    fun deleteAccount() {
        val email = authUserEmail ?: return
        val authToken = token ?: return
        viewModelScope.launch {
            TapcardApi.deleteAccount(authToken, email)
                .onSuccess { logout() }
                .onFailure { activeAlert = AppAlert("Couldn't delete account", it.message ?: "Please try again.") }
        }
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

    /**
     * A QR / barcode was detected live from the camera stream — go straight to
     * review with its details. Guarded so late frames can't re-trigger after we
     * navigate away from the scanner.
     */
    fun onScannedCard(card: DigitalCard) {
        if (screen != AppScreen.SCAN) return
        contactManualEntry = false
        isScanning = true
        viewModelScope.launch {
            val enriched = enrichIfTapcardLink(card)
            isScanning = false
            draft = enriched
            screen = scanDestination()
        }
    }

    /**
     * If a scanned QR was really a Tapcard web-card link (…/c/slug), pull the full
     * card from the backend so the review shows the person's real details — not
     * just the bare URL. Falls back to the original card if the lookup fails.
     */
    private suspend fun enrichIfTapcardLink(card: DigitalCard): DigitalCard {
        val slug = Regex("/c/([A-Za-z0-9_-]+)").find(card.website)?.groupValues?.get(1)
            ?: return card
        return TapcardApi.fetchPublicCard(slug).getOrNull() ?: card
    }

    fun onImageCaptured(uri: Uri) {
        contactManualEntry = false
        isScanning = true
        viewModelScope.launch {
            val result = runCatching {
                // Universal scan: try a QR/barcode first (vCard/MECARD/profile link),
                // then fall back to reading the printed text (OCR).
                val base = CardScanner.scanCardFromQr(getApplication(), uri)
                    ?: CardParser.parse(
                        withContext(Dispatchers.Default) { CardScanner.recognize(getApplication(), uri) },
                    )
                // If it was a Tapcard link, pull the full card behind that slug.
                enrichIfTapcardLink(base)
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
                screen = scanDestination()
            }.onFailure {
                activeAlert = AppAlert("Scan failed", it.message ?: "Could not process the image.")
                draft = DigitalCard()
                screen = scanDestination()
            }
        }
    }

    /** Saves a scanned person to Contacts, then jumps to the Contacts list. */
    fun saveScannedContact(contact: Contact) {
        val t = token ?: run {
            activeAlert = AppAlert("Sign in needed", "Sign in to save contacts to your account.")
            return
        }
        if (contact.name.isBlank() && contact.company.isBlank() &&
            contact.email.isBlank() && contact.phone.isBlank()
        ) {
            activeAlert = AppAlert("Nothing to save", "This scan didn't capture any usable details.")
            return
        }
        isSaving = true
        viewModelScope.launch {
            TapcardApi.createContact(t, contact)
                .onSuccess {
                    isSaving = false
                    refreshPeople()
                    screen = AppScreen.CONTACTS
                }
                .onFailure {
                    isSaving = false
                    activeAlert = AppAlert("Couldn't save", it.message ?: "Please try again.")
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
            val result = TapcardApi.publish(token, card)
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

    private companion object {
        /** Minimum time the boot splash stays up, so it never flashes on a fast path. */
        const val BOOT_MIN_MS = 800L
    }
}