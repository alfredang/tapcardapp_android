package com.tertiaryinfotech.tapcard.net

/**
 * Backend connection settings.
 *
 * For LOCAL testing, point BASE_URL at the machine running the web backend:
 *   • Android emulator : "http://10.0.2.2:3000"  (10.0.2.2 = the host PC's localhost)
 *   • Physical device  : "http://<your-PC-LAN-IP>:3000" (phone + PC on same Wi-Fi;
 *                          also add that IP to res/xml/network_security_config.xml)
 *
 * For PRODUCTION, use: "https://tapcard.tertiaryinfotech.com"
 */
object ApiConfig {
    // PRODUCTION backend. For LOCAL testing instead, point this at the machine
    // running the web backend, e.g. "http://10.0.2.2:3000" (emulator) or
    // "http://<your-PC-LAN-IP>:3000" (physical device on the same Wi-Fi — that
    // IP must also be listed in res/xml/network_security_config.xml).
    const val BASE_URL = "https://tapcard.tertiaryinfotech.com"

    /**
     * Public base for shareable card links (NFC tap, QR, etc.). Unlike BASE_URL
     * this must be a real internet address, since recipients open it off-network.
     */
    const val PUBLIC_WEB_URL = "https://tapcard.tertiaryinfotech.com"
}