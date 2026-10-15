package com.tertiaryinfotech.tapcard.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Mirrors the web's internationalDigits cases — the WhatsApp QR depends on these. */
class PhoneNumberTest {

    private fun intl(raw: String, cc: String = "65") = PhoneNumber.internationalDigits(raw, cc)

    @Test fun localSingaporeNumberGetsCountryCode() = assertEquals("6596983731", intl("96983731"))
    @Test fun spacedLocalNumber() = assertEquals("6596983731", intl("9698 3731"))
    @Test fun plusPrefixKeptAsIs() = assertEquals("60123456789", intl("+60 12-345 6789"))
    @Test fun doubleZeroPrefixStripped() = assertEquals("60123456789", intl("0060123456789"))
    @Test fun trunkZeroReplacedByCountryCode() = assertEquals("60123456789", intl("012-345 6789", "60"))
    @Test fun numberAlreadyCarryingCountryCode() = assertEquals("6596983731", intl("6596983731"))
    @Test fun indianLocalStartingWith91NotMistakenForCode() = assertEquals("919123456789", intl("9123456789", "91"))
    @Test fun blankStaysBlank() = assertEquals("", intl("  "))
    @Test fun internationalAddsPlus() = assertEquals("+6596983731", PhoneNumber.international("96983731"))
}
