# Google Play Store — Setup & Submission Guide (Tapcard)

This is the end-to-end guide to ship **Tapcard** to Google Play. Steps that **only you** can do
(they need your Google login) are marked **[YOU]**; everything else is already done in this repo or
scripted.

- **Package name:** `com.tertiaryinfotech.tapcard`
- **App name:** Tapcard — Digital Business Card
- **Developer:** Tertiary Infotech Academy Pte. Ltd.

> Unlike some of our other apps, Tapcard needs **no API keys** and **no background services** — the
> only runtime permission is the camera, and all text recognition runs on-device.

---

## 1. Create your upload keystore  *(scripted — run once)*

Google Play apps must be signed. Run the helper (it generates a keystore and `keystore.properties`,
both gitignored):

```bash
cd mobile/Android/tapcardapp
./scripts/make_keystore.sh
```

It will prompt for a password (or accept one as `$1`). Output:
- `keystore/tapcard-release.jks` — **back this up somewhere safe.** It is your *upload key*.
- `keystore/keystore.properties` — passwords read by Gradle at build time.

---

## 2. Build the release App Bundle (.aab)  *(scripted)*

```bash
./gradlew :app:bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Get your **upload SHA-1 / SHA-256** any time with:

```bash
keytool -list -v -keystore keystore/tapcard-release.jks -alias tapcard | grep -E "SHA1|SHA256"
```

---

## 3. Play Console — first-time app setup  **[YOU]**

> The Play Console has no public API for *creating* an app or filling these forms, so these are
> manual web steps. Answers tailored to this app are below.

1. Go to <https://play.google.com/console> (you need a **Play Developer account**, US$25 one-time).
2. **Create app:**
   - App name: **Tapcard — Digital Business Card**
   - Default language: English (US)
   - App or game: **App**
   - Free or paid: **Free**
   - Accept the declarations.
3. **Set up your app → App access:** *All functionality is available without special access.*
4. **Ads:** *No, this app does not contain ads.*
5. **Content rating** (questionnaire). Answers for this app:
   - Category: **Utility / Productivity** (no violence, no sexual content, no profanity, no
     controlled substances, no gambling, no user-generated content shared through the app).
     Result: **Everyone / PEGI 3**.
6. **Target audience and content:** target age **18+** (or 13+; it's a general productivity tool).
   Not directed to children.
7. **Data safety** (the important one). For this app it's about as simple as it gets:
   - **Does your app collect or share any of the required user data types?** → **No.**
     The camera image is processed on-device and not transmitted; card details are stored locally.
   - No accounts, no ads, no analytics, no third-party SDKs, no network backend.
   - A ready-to-paste summary is in **§5** below.
8. **Privacy policy:** a hosted URL is required. A ready-made policy is in
   [`store/privacy-policy.md`](store/privacy-policy.md) — host it (GitHub Pages, your site, etc.)
   and paste the URL.
9. **Government / Financial / Health apps:** No to all.

---

## 4. Store listing  **[YOU] paste the prepared copy**

**Main store listing** (copy from [`store/listing.md`](store/listing.md)):
- App name: Tapcard — Digital Business Card
- Short description (≤80 chars)
- Full description (≤4000 chars)
- App icon: 512×512 PNG → `store/assets/icon-512.png`
- Feature graphic: 1024×500 PNG → `store/assets/feature-1024x500.png`
- Phone screenshots: `store/assets/screenshots/` (min 2)
- Category: **Business**
- Contact email: angch@tertiaryinfotech.com

---

## 5. Create a release  **[YOU]**

### Closed testing (the chosen track — "All testers" for review)

1. **Test and release → Testing → Closed testing**.
2. On the default **Alpha** track (or **Create track**), click **Create new release**.
3. **App bundles:** upload `app/build/outputs/bundle/release/app-release.aab`.
   Accept **Play App Signing** when prompted.
4. **Release name:** `1.0 (1)`. **Release notes:** e.g.
   `First closed-testing build of Tapcard — scan business cards and share digital cards via QR.`
5. **Testers:** open the **Testers** tab for the track → add an email list (a Google Group or a
   list that includes **all your testers**), so every tester can access the build. Save.
6. Click **Next → Save → Review release → Start rollout to Closed testing**, then **Send for review**.
7. Share the **opt-in URL** (Testers tab) with your testers so they can install once approved.

> Internal testing is even faster (no review wait) if you just want to smoke-test first; the steps
> are identical under **Testing → Internal testing**.

### Production (later)

When happy: **Production → Create new release**, upload the same AAB (or a new versionCode),
complete the release notes, and **Send for review**.

> **Play App Signing:** accept it when prompted (default). Google holds the *app signing key*; your
> `.jks` is only the *upload key*.

---

## 6. Data safety — copy/paste summary

```
Data collected: None.
The camera is used only to photograph a business card; the image is read on-device by Android's
on-device text recognizer and is not transmitted anywhere. Card details you create are stored
locally on the device only.
No accounts, no ads, no analytics, no third-party data sharing, no network backend.
Data is removed on uninstall or by deleting a card in the app.
```

---

## Quick command reference

```bash
# Debug build + install on connected phone/emulator
./gradlew :app:installDebug

# Release bundle for Play
./gradlew :app:bundleRelease

# Show upload-key SHA-1 / SHA-256 (for Play App Signing)
keytool -list -v -keystore keystore/tapcard-release.jks -alias tapcard
```
