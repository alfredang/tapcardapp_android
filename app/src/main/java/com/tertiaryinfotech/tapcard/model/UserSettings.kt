package com.tertiaryinfotech.tapcard.model

import com.tertiaryinfotech.tapcard.util.PhoneNumber
import com.tertiaryinfotech.tapcard.util.QrMode
import kotlinx.serialization.Serializable

/**
 * Account preferences edited in the app's Settings and on the web's /settings
 * page — both read and write the same User columns via `/api/mobile/settings`,
 * so a change on either shows up on the other.
 */
@Serializable
data class UserSettings(
    /** Default dialling code, digits only ("65"). Turns local numbers into working wa.me links. */
    val countryCode: String = PhoneNumber.DEFAULT_COUNTRY_CODE,
    /** What the card's QR opens by default: WHATSAPP | LINKEDIN | CARD. */
    val qrMode: String = QrMode.WHATSAPP.serverValue,
) {
    val defaultQrMode: QrMode get() = QrMode.fromServer(qrMode)
}
