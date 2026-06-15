package com.tertiaryinfotech.tapcard.util

import com.tertiaryinfotech.tapcard.model.DigitalCard

/**
 * Turns the raw lines of text recognised on a business card into a structured
 * [DigitalCard]. The same role as the iOS app's on-device parser: regexes pick
 * out the unambiguous fields (email, phone, website), then simple heuristics
 * assign the remaining lines to name / title / company.
 */
object CardParser {

    private val emailRegex = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val urlRegex = Regex("""(https?://)?(www\.)?[A-Za-z0-9\-]+\.[A-Za-z]{2,}(/\S*)?""")
    // 7+ digits allowing spaces, dashes, dots, parens and a leading +.
    private val phoneRegex = Regex("""\+?\(?\d[\d\s().\-]{6,}\d""")

    private val titleKeywords = listOf(
        "manager", "director", "engineer", "developer", "ceo", "cto", "cfo", "coo",
        "founder", "president", "officer", "consultant", "analyst", "designer",
        "head", "lead", "executive", "specialist", "coordinator", "architect",
        "administrator", "supervisor", "partner", "associate", "owner", "sales",
        "marketing", "account", "vp", "vice president",
    )
    private val companyKeywords = listOf(
        "ltd", "limited", "llc", "inc", "incorporated", "corp", "corporation",
        "pte", "co.", "company", "group", "technologies", "technology", "solutions",
        "services", "systems", "consulting", "enterprise", "holdings", "industries",
        "academy", "studio", "labs", "global", "international",
    )

    fun parse(rawText: String): DigitalCard {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var email = ""
        var phone = ""
        var website = ""
        val leftovers = mutableListOf<String>()

        for (line in lines) {
            val lower = line.lowercase()
            val emailMatch = emailRegex.find(line)
            val phoneMatch = phoneRegex.find(line)
            when {
                email.isBlank() && emailMatch != null -> email = emailMatch.value
                phone.isBlank() && phoneMatch != null && !line.contains("@") &&
                    phoneMatch.value.count { it.isDigit() } in 7..15 -> phone = phoneMatch.value.trim()
                website.isBlank() && urlRegex.matches(line) && !line.contains("@") &&
                    (lower.startsWith("www") || lower.startsWith("http") ||
                        lower.endsWith(".com") || lower.endsWith(".io") ||
                        lower.endsWith(".net") || lower.endsWith(".org") ||
                        lower.contains(".com/")) -> website = line
                else -> leftovers.add(line)
            }
        }

        // Heuristics over the remaining lines.
        var name = ""
        var title = ""
        var company = ""
        val addressParts = mutableListOf<String>()

        for (line in leftovers) {
            val lower = line.lowercase()
            when {
                title.isBlank() && titleKeywords.any { lower.contains(it) } &&
                    !companyKeywords.any { lower.contains(it) } -> title = line
                company.isBlank() && companyKeywords.any { lower.contains(it) } -> company = line
                name.isBlank() && looksLikeName(line) -> name = line
                else -> addressParts.add(line)
            }
        }

        // If nothing matched a name yet, take the first leftover line.
        if (name.isBlank()) {
            leftovers.firstOrNull { it != title && it != company }?.let {
                name = it
                addressParts.remove(it)
            }
        }

        val address = addressParts
            .filter { it != name && it != title && it != company }
            .joinToString(", ")
            .take(180)

        return DigitalCard(
            name = name,
            title = title,
            company = company,
            phone = phone,
            email = email,
            website = website,
            address = address,
        )
    }

    /** A short line of mostly letters with 1–4 words reads like a person's name. */
    private fun looksLikeName(line: String): Boolean {
        if (line.any { it.isDigit() }) return false
        val words = line.split(" ").filter { it.isNotBlank() }
        if (words.size !in 1..4) return false
        val letters = line.count { it.isLetter() }
        return letters >= line.length * 0.6
    }
}
