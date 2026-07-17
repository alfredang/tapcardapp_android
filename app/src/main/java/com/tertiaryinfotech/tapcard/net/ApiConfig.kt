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
    // Android emulator → the host PC's localhost (this is the emulator's alias).
    // Physical device instead? Use "http://<your-PC-LAN-IP>:3000" (e.g. 192.168.3.29)
    // and make sure that IP is in res/xml/network_security_config.xml.
    // Production: "https://tapcard.tertiaryinfotech.com".
    // Physical device on same Wi-Fi as the PC running the backend. For the
    // Android emulator instead, use "http://10.0.2.2:3000".
    const val BASE_URL = "http://192.168.3.43:3000"

    /**
     * Public base for shareable card links (NFC tap, QR, etc.). Unlike BASE_URL
     * this must be a real internet address, since recipients open it off-network.
     */
    const val PUBLIC_WEB_URL = "https://tapcard.tertiaryinfotech.com"
}