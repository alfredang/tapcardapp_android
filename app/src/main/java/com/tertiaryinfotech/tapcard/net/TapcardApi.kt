package com.tertiaryinfotech.tapcard.net

import com.tertiaryinfotech.tapcard.model.AnalyticsSummary
import com.tertiaryinfotech.tapcard.model.Contact
import com.tertiaryinfotech.tapcard.model.DigitalCard
import com.tertiaryinfotech.tapcard.model.Lead
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
import com.tertiaryinfotech.tapcard.model.CardLink
import java.net.HttpURLConnection
import java.net.URL

/** Signed-in identity returned by login/register. */
data class AuthResult(val token: String, val name: String?, val email: String?)

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

    suspend fun publish(card: DigitalCard): Result<PublishResult> = runCatching {
        val (code, text) = request("POST", "/api/mobile/onboard", body = card.toOnboardJson())
        if (code !in 200..299) throw Exception(errorFrom(text, code))
        val res = json.decodeFromString<OnboardResponse>(text)
        val url = res.card?.url ?: throw Exception("The server didn't return a card link.")
        PublishResult(url, res.card.slug.orEmpty(), res.isNewAccount, res.password)
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Serializable
    private data class AuthUser(val id: String? = null, val name: String? = null, val email: String? = null)

    @Serializable
    private data class AuthResponse(val token: String? = null, val user: AuthUser? = null)

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
        val introVideo: String? = null,
        val gallery: List<String>? = null,
        val links: List<ServerLink>? = null,
        val theme: String? = null,
        val accentColor: String? = null,
    )

    @Serializable
    private data class ServerLink(
        val label: String = "",
        val url: String = "",
        val kind: String = "LINK",
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
        introVideo = introVideo.orEmpty(),
        gallery = gallery?.filter { it.isNotBlank() } ?: emptyList(),
        links = links?.map { CardLink(it.label, it.url, it.kind) } ?: emptyList(),
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
        if (profilePhoto.isNotBlank()) put("profilePhoto", profilePhoto.trim())
        if (companyLogo.isNotBlank()) put("companyLogo", companyLogo.trim())
        if (coverBanner.isNotBlank()) put("coverBanner", coverBanner.trim())
        if (introVideo.isNotBlank()) put("introVideo", introVideo.trim())
        putJsonArray("gallery") {
            gallery.filter { it.isNotBlank() }.forEach { add(it.trim()) }
        }
        putJsonArray("links") {
            links.filter { it.url.isNotBlank() }.forEach { link ->
                addJsonObject {
                    put("label", link.label.trim())
                    put("url", link.url.trim())
                    put("kind", link.kind)
                }
            }
        }
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
