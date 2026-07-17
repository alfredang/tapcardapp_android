package com.tertiaryinfotech.tapcard.net

import com.tertiaryinfotech.tapcard.model.AnalyticsSummary
import com.tertiaryinfotech.tapcard.model.Appointment
import com.tertiaryinfotech.tapcard.model.Contact
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.model.Lead
import com.tertiaryinfotech.tapcard.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.HttpURLConnection
import java.net.URL

/** Signed-in identity returned by login/register/OTP verify. */
data class AuthResult(val token: String, val name: String?, val email: String?)

/** Result of requesting an email OTP — whether the email is a brand-new account. */
data class OtpRequestResult(val isNewAccount: Boolean)

/** What the app gets back after publishing a card via the onboard endpoint. */
data class PublishResult(
    val url: String,
    val slug: String,
    val isNewAccount: Boolean,
    val generatedPassword: String?,
)

/**
 * Client for the shared Tapcard web backend (same Coolify/Postgres the web app
 * uses). Covers mobile auth (login/register → bearer token) and token-based
 * two-way card sync, plus the unauthenticated onboard publish.
 */
object TapcardApi {

    private val json = Json { ignoreUnknownKeys = true }

    // ─── HTTP core ──────────────────────────────────────────────────────────

    private suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null,
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val conn = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        code to text
    }

    private fun errorFrom(bodyText: String, code: Int): String =
        runCatching { json.decodeFromString<ErrorResponse>(bodyText).error }.getOrNull()
            ?: "Server error ($code)"

    // ─── Auth ───────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<AuthResult> = runCatching {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }.toString()
        val (code, text) = request("POST", "/api/mobile/login", body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        val res = json.decodeFromString<AuthResponse>(text)
        AuthResult(res.token ?: throw Exception("No token returned"), res.user?.name, res.user?.email)
    }

    suspend fun register(name: String, email: String, password: String): Result<AuthResult> = runCatching {
        val payload = buildJsonObject {
            put("name", name.trim())
            put("email", email.trim())
            put("password", password)
        }.toString()
        val (code, text) = request("POST", "/api/mobile/register", body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        val res = json.decodeFromString<AuthResponse>(text)
        AuthResult(res.token ?: throw Exception("No token returned"), res.user?.name, res.user?.email)
    }

    // ─── Passwordless email OTP ─────────────────────────────────────────────

    /** Requests a 6-digit code by email. Returns whether this email is new. */
    suspend fun requestOtp(email: String): Result<OtpRequestResult> = runCatching {
        val payload = buildJsonObject { put("email", email.trim()) }.toString()
        val (code, text) = request("POST", "/api/mobile/otp/request", body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        OtpRequestResult(json.decodeFromString<OtpRequestResponse>(text).isNewAccount)
    }

    /** Verifies the emailed code; creates the account if new. Returns a token. */
    suspend fun verifyOtp(email: String, code: String, name: String?): Result<AuthResult> = runCatching {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("code", code.trim())
            if (!name.isNullOrBlank()) put("name", name.trim())
        }.toString()
        val (status, text) = request("POST", "/api/mobile/otp/verify", body = payload)
        if (status !in 200..299) throw Exception(errorFrom(text, status))
        val res = json.decodeFromString<AuthResponse>(text)
        AuthResult(res.token ?: throw Exception("No token returned"), res.user?.name, res.user?.email)
    }

    // ─── Public card lookup (scanned QR / link) ─────────────────────────────

    /** Fetches a published card by its public slug — used after scanning a QR. */
    suspend fun fetchPublicCard(slug: String): Result<DigitalCard> = runCatching {
        val (code, text) = request("GET", "/api/c/$slug")
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<CardResponse>(text).card?.toDigitalCard()
            ?: throw Exception("Card not found")
    }

    // ─── Card sync ──────────────────────────────────────────────────────────

    suspend fun fetchCards(token: String): Result<List<DigitalCard>> = runCatching {
        val (code, text) = request("GET", "/api/mobile/cards", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<CardsResponse>(text).cards.map { it.toDigitalCard() }
    }

    suspend fun createCard(token: String, card: DigitalCard): Result<DigitalCard> = runCatching {
        val (code, text) = request("POST", "/api/mobile/cards", token = token, body = card.toServerJson())
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<CardResponse>(text).card?.toDigitalCard()
            ?: throw Exception("Malformed response")
    }

    suspend fun updateCard(token: String, card: DigitalCard): Result<DigitalCard> = runCatching {
        val (code, text) = request("PATCH", "/api/mobile/cards/${card.id}", token = token, body = card.toServerJson())
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<CardResponse>(text).card?.toDigitalCard()
            ?: throw Exception("Malformed response")
    }

    suspend fun deleteCard(token: String, id: String): Result<Unit> = runCatching {
        val (code, text) = request("DELETE", "/api/mobile/cards/$id", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
    }

    // ─── Contacts sync ────────────────────────────────────────────────────────

    suspend fun fetchContacts(token: String): Result<List<Contact>> = runCatching {
        val (code, text) = request("GET", "/api/mobile/contacts", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<ContactsResponse>(text).contacts.map { it.toContact() }
    }

    suspend fun createContact(token: String, contact: Contact): Result<Contact> = runCatching {
        val (code, text) = request("POST", "/api/mobile/contacts", token = token, body = contact.toServerJson())
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<ContactResponse>(text).contact?.toContact()
            ?: throw Exception("Malformed response")
    }

    suspend fun updateContact(token: String, contact: Contact): Result<Contact> = runCatching {
        val (code, text) = request("PATCH", "/api/mobile/contacts/${contact.id}", token = token, body = contact.toServerJson())
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<ContactResponse>(text).contact?.toContact()
            ?: throw Exception("Malformed response")
    }

    suspend fun deleteContact(token: String, id: String): Result<Unit> = runCatching {
        val (code, text) = request("DELETE", "/api/mobile/contacts/$id", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
    }

    // ─── Leads inbox (captured from your web cards) ─────────────────────────────

    suspend fun fetchLeads(token: String): Result<List<Lead>> = runCatching {
        val (code, text) = request("GET", "/api/mobile/leads", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<LeadsResponse>(text).leads.map { it.toLead() }
    }

    suspend fun deleteLead(token: String, id: String): Result<Unit> = runCatching {
        val (code, text) = request("DELETE", "/api/mobile/leads/$id", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
    }

    // ─── Planner: tasks ─────────────────────────────────────────────────────────

    suspend fun fetchTasks(token: String): Result<List<Task>> = runCatching {
        val (code, text) = request("GET", "/api/mobile/tasks", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<TasksResponse>(text).tasks
    }

    suspend fun createTask(token: String, title: String, type: String, dueAt: String?): Result<Task> = runCatching {
        val payload = buildJsonObject {
            put("title", title.trim())
            put("type", type)
            if (!dueAt.isNullOrBlank()) put("dueAt", dueAt)
        }.toString()
        val (code, text) = request("POST", "/api/mobile/tasks", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<TaskResponse>(text).task ?: throw Exception("Malformed response")
    }

    suspend fun setTaskStatus(token: String, id: String, status: String): Result<Task> = runCatching {
        val payload = buildJsonObject { put("status", status) }.toString()
        val (code, text) = request("PATCH", "/api/mobile/tasks/$id", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<TaskResponse>(text).task ?: throw Exception("Malformed response")
    }

    suspend fun updateTask(token: String, id: String, title: String, type: String, dueAt: String?): Result<Task> = runCatching {
        val payload = buildJsonObject {
            put("title", title.trim())
            put("type", type)
            put("dueAt", dueAt)
        }.toString()
        val (code, text) = request("PATCH", "/api/mobile/tasks/$id", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<TaskResponse>(text).task ?: throw Exception("Malformed response")
    }

    suspend fun deleteTask(token: String, id: String): Result<Unit> = runCatching {
        val (code, text) = request("DELETE", "/api/mobile/tasks/$id", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
    }

    // ─── Planner: appointments ──────────────────────────────────────────────────

    suspend fun fetchAppointments(token: String): Result<List<Appointment>> = runCatching {
        val (code, text) = request("GET", "/api/mobile/appointments", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<AppointmentsResponse>(text).appointments
    }

    suspend fun createAppointment(
        token: String,
        name: String,
        email: String?,
        startAt: String,
        endAt: String,
        notes: String?,
    ): Result<Appointment> = runCatching {
        val payload = buildJsonObject {
            put("name", name.trim())
            if (!email.isNullOrBlank()) put("email", email.trim())
            put("startAt", startAt)
            put("endAt", endAt)
            if (!notes.isNullOrBlank()) put("notes", notes.trim())
        }.toString()
        val (code, text) = request("POST", "/api/mobile/appointments", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<AppointmentResponse>(text).appointment ?: throw Exception("Malformed response")
    }

    suspend fun updateAppointment(
        token: String,
        id: String,
        name: String,
        email: String?,
        startAt: String,
        endAt: String,
        notes: String?,
    ): Result<Appointment> = runCatching {
        val payload = buildJsonObject {
            put("name", name.trim())
            put("email", email?.trim() ?: "")
            put("startAt", startAt)
            put("endAt", endAt)
            put("notes", notes?.trim() ?: "")
        }.toString()
        val (code, text) = request("PATCH", "/api/mobile/appointments/$id", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<AppointmentResponse>(text).appointment ?: throw Exception("Malformed response")
    }

    suspend fun deleteAppointment(token: String, id: String): Result<Unit> = runCatching {
        val (code, text) = request("DELETE", "/api/mobile/appointments/$id", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
    }

    // ─── Analytics summary ──────────────────────────────────────────────────────

    suspend fun fetchAnalytics(token: String): Result<AnalyticsSummary> = runCatching {
        val (code, text) = request("GET", "/api/mobile/analytics", token = token)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<AnalyticsSummary>(text)
    }

    // ─── AI text generation ─────────────────────────────────────────────────────

    /** Drafts card text via the backend AI. [action] is "bio", "about", "company" or "pitch". */
    suspend fun generateText(
        token: String,
        action: String,
        fullName: String,
        jobTitle: String,
        company: String,
    ): Result<String> = runCatching {
        val payload = buildJsonObject {
            put("action", action)
            if (fullName.isNotBlank()) put("fullName", fullName.trim())
            if (jobTitle.isNotBlank()) put("jobTitle", jobTitle.trim())
            if (company.isNotBlank()) put("company", company.trim())
        }.toString()
        val (code, text) = request("POST", "/api/mobile/ai", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        json.decodeFromString<AiResponse>(text).text?.takeIf { it.isNotBlank() }
            ?: throw Exception("The AI didn't return any text. Try again.")
    }

    // ─── Unauthenticated publish (onboard) ────────────────────────────────────

    suspend fun publish(token: String?, card: DigitalCard): Result<PublishResult> = runCatching {
        val (code, text) = request("POST", "/api/mobile/onboard", token = token, body = card.toOnboardJson())
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        val res = json.decodeFromString<OnboardResponse>(text)
        val url = res.card?.url ?: throw Exception("The server didn't return a card link.")
        PublishResult(url, res.card.slug.orEmpty(), res.isNewAccount, res.password)
    }

    // ─── Account deletion ─────────────────────────────────────────────────────

    /**
     * Permanently deletes the signed-in account. Sends the login [token] so the
     * backend can verify ownership from the token itself (email is included only
     * as a fallback for the legacy shared-key path).
     */
    suspend fun deleteAccount(token: String, email: String): Result<Unit> = runCatching {
        val payload = buildJsonObject { put("email", email.trim()) }.toString()
        val (code, text) = request("POST", "/api/mobile/delete-account", token = token, body = payload)
        if (code !in 200..299) throw Exception(errorFrom(text, code))
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Serializable
    private data class AuthUser(val id: String? = null, val name: String? = null, val email: String? = null)

    @Serializable
    private data class AuthResponse(val token: String? = null, val user: AuthUser? = null)

    @Serializable
    private data class OtpRequestResponse(val ok: Boolean = false, val isNewAccount: Boolean = false)

    @Serializable
    private data class ErrorResponse(val error: String? = null)

    @Serializable
    private data class AiResponse(val text: String? = null)

    @Serializable
    private data class ServerCard(
        val id: String,
        val slug: String? = null,
        val fullName: String = "",
        val jobTitle: String? = null,
        val company: String? = null,
        val department: String? = null,
        val tagline: String? = null,
        val bio: String? = null,
        val about: String? = null,
        val mobile: String? = null,
        val officePhone: String? = null,
        val whatsapp: String? = null,
        val email: String? = null,
        val website: String? = null,
        val address: String? = null,
        val linkedin: String? = null,
        val facebook: String? = null,
        val instagram: String? = null,
        val tiktok: String? = null,
        val twitter: String? = null,
        val telegram: String? = null,
        val youtube: String? = null,
        val profilePhoto: String? = null,
        val companyLogo: String? = null,
        val coverBanner: String? = null,
        val theme: String? = null,
        val accentColor: String? = null,
    )

    @Serializable
    private data class CardsResponse(val cards: List<ServerCard> = emptyList())

    @Serializable
    private data class CardResponse(val card: ServerCard? = null, val url: String? = null)

    @Serializable
    private data class ServerContact(
        val id: String,
        val name: String = "",
        val company: String? = null,
        val position: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val whatsapp: String? = null,
        val address: String? = null,
        val notes: String? = null,
        val tags: String? = null,
    )

    @Serializable
    private data class ContactsResponse(val contacts: List<ServerContact> = emptyList())

    @Serializable
    private data class ContactResponse(val contact: ServerContact? = null)

    @Serializable
    private data class ServerLead(
        val id: String,
        val name: String = "",
        val email: String? = null,
        val phone: String? = null,
        val company: String? = null,
        val message: String? = null,
        val source: String? = null,
        val createdAt: String? = null,
    )

    @Serializable
    private data class LeadsResponse(val leads: List<ServerLead> = emptyList())

    @Serializable
    private data class TasksResponse(val tasks: List<Task> = emptyList())

    @Serializable
    private data class TaskResponse(val task: Task? = null)

    @Serializable
    private data class AppointmentsResponse(val appointments: List<Appointment> = emptyList())

    @Serializable
    private data class AppointmentResponse(val appointment: Appointment? = null)

    @Serializable
    private data class OnboardResponse(
        val ok: Boolean = false,
        val isNewAccount: Boolean = false,
        val email: String? = null,
        val password: String? = null,
        val card: CardInfo? = null,
    ) {
        @Serializable
        data class CardInfo(val id: String? = null, val slug: String? = null, val url: String? = null)
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private fun ServerCard.toDigitalCard() = DigitalCard(
        id = id,
        name = fullName,
        title = jobTitle.orEmpty(),
        company = company.orEmpty(),
        department = department.orEmpty(),
        tagline = tagline.orEmpty(),
        bio = bio.orEmpty(),
        about = about.orEmpty(),
        phone = mobile.orEmpty(),
        officePhone = officePhone.orEmpty(),
        whatsapp = whatsapp.orEmpty(),
        email = email.orEmpty(),
        website = website.orEmpty(),
        address = address.orEmpty(),
        linkedin = linkedin.orEmpty(),
        facebook = facebook.orEmpty(),
        instagram = instagram.orEmpty(),
        tiktok = tiktok.orEmpty(),
        twitter = twitter.orEmpty(),
        telegram = telegram.orEmpty(),
        youtube = youtube.orEmpty(),
        profilePhoto = profilePhoto.orEmpty(),
        companyLogo = companyLogo.orEmpty(),
        coverBanner = coverBanner.orEmpty(),
        theme = theme?.ifBlank { "MODERN" } ?: "MODERN",
        accentColor = parseHexColor(accentColor),
        slug = slug.orEmpty(),
        // Non-zero marks this as an existing (server) card, so saves PATCH not POST.
        createdAtEpoch = System.currentTimeMillis(),
    )

    private fun DigitalCard.toServerJson(): String = buildJsonObject {
        put("fullName", name.trim())
        if (email.isNotBlank()) put("email", email.trim())
        if (title.isNotBlank()) put("jobTitle", title.trim())
        if (company.isNotBlank()) put("company", company.trim())
        if (department.isNotBlank()) put("department", department.trim())
        if (tagline.isNotBlank()) put("tagline", tagline.trim())
        if (bio.isNotBlank()) put("bio", bio.trim())
        if (about.isNotBlank()) put("about", about.trim())
        if (phone.isNotBlank()) put("mobile", phone.trim())
        if (officePhone.isNotBlank()) put("officePhone", officePhone.trim())
        if (whatsapp.isNotBlank()) put("whatsapp", whatsapp.trim())
        if (address.isNotBlank()) put("address", address.trim())
        normalizeUrl(website)?.let { put("website", it) }
        if (linkedin.isNotBlank()) put("linkedin", linkedin.trim())
        if (facebook.isNotBlank()) put("facebook", facebook.trim())
        if (instagram.isNotBlank()) put("instagram", instagram.trim())
        if (tiktok.isNotBlank()) put("tiktok", tiktok.trim())
        if (twitter.isNotBlank()) put("twitter", twitter.trim())
        if (telegram.isNotBlank()) put("telegram", telegram.trim())
        if (youtube.isNotBlank()) put("youtube", youtube.trim())
        // Always send media fields (even when blank) so REMOVING an image actually
        // clears it server-side — a partial update skips omitted fields.
        put("profilePhoto", profilePhoto.trim())
        put("companyLogo", companyLogo.trim())
        put("coverBanner", coverBanner.trim())
        put("theme", theme.ifBlank { "MODERN" })
        put("accentColor", toHexColor(accentColor))
    }.toString()

    private fun DigitalCard.toOnboardJson(): String = buildJsonObject {
        put("fullName", name.trim())
        put("email", email.trim())
        if (title.isNotBlank()) put("jobTitle", title.trim())
        if (company.isNotBlank()) put("company", company.trim())
        if (phone.isNotBlank()) put("mobile", phone.trim())
        if (address.isNotBlank()) put("address", address.trim())
        normalizeUrl(website)?.let { put("website", it) }
    }.toString()

    private fun ServerContact.toContact() = Contact(
        id = id,
        name = name,
        company = company.orEmpty(),
        position = position.orEmpty(),
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        whatsapp = whatsapp.orEmpty(),
        address = address.orEmpty(),
        notes = notes.orEmpty(),
        tags = tags.orEmpty(),
    )

    private fun Contact.toServerJson(): String = buildJsonObject {
        put("name", name.trim())
        if (company.isNotBlank()) put("company", company.trim())
        if (position.isNotBlank()) put("position", position.trim())
        if (email.isNotBlank()) put("email", email.trim())
        if (phone.isNotBlank()) put("phone", phone.trim())
        if (whatsapp.isNotBlank()) put("whatsapp", whatsapp.trim())
        if (address.isNotBlank()) put("address", address.trim())
        if (notes.isNotBlank()) put("notes", notes.trim())
        if (tags.isNotBlank()) put("tags", tags.trim())
    }.toString()

    private fun ServerLead.toLead() = Lead(
        id = id,
        name = name,
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        company = company.orEmpty(),
        message = message.orEmpty(),
        source = source ?: "card",
        createdAt = createdAt.orEmpty(),
    )

    private fun toHexColor(argb: Long): String = "#%06X".format(argb and 0xFFFFFF)

    private fun parseHexColor(hex: String?): Long {
        val h = hex?.trim()?.removePrefix("#") ?: return 0xFF7C5CFF
        return runCatching { 0xFF000000 or (h.toLong(16) and 0xFFFFFF) }.getOrDefault(0xFF7C5CFF)
    }

    private fun normalizeUrl(raw: String): String? {
        val v = raw.trim()
        if (v.isBlank()) return null
        return if (v.startsWith("http://") || v.startsWith("https://")) v else "https://$v"
    }
}
