# Local testing — Android app + web backend

This walks you through running the **web backend on your PC** and pointing the
**Android app** at it, so you can test login + card sync end-to-end before
anything is deployed.

The mobile API lives on the `mobile-api` branch of the web repo, already checked
out at `C:\Users\amand\StudioProjects\tapcard-web`.

---

## 1. Start the web backend locally

Open a terminal in the web repo:

```bash
cd C:\Users\amand\StudioProjects\tapcard-web
```

**a. Create the env file** (once). Create `.env` with at least:

```
AUTH_SECRET="any-long-random-string-here"
DATABASE_URL="postgresql://postgres:postgres@localhost:5434/business_cards?schema=public"
NEXT_PUBLIC_APP_URL="http://localhost:3000"
```

(Generate a secret with: `openssl rand -base64 32`)

**b. Start Postgres** (Docker):

```bash
docker compose up -d db
```

**c. Set up the database + demo data** (once):

```bash
npm install
npm run setup
```

**d. Run the server:**

```bash
npm run dev
```

Leave this running. The backend is now at **http://localhost:3000**.
Quick check: open http://localhost:3000 in a browser — you should see the site.

> A demo account already exists: `demo@tapcard.app` / `password123`.

---

## 2. Point the Android app at your PC

Already configured for the **Android emulator** — `app/.../net/ApiConfig.kt` uses
`http://10.0.2.2:3000` (from inside the emulator, `10.0.2.2` = your PC's
`localhost`). No change needed if you run on the emulator.

**Physical phone instead?** Put your PC and phone on the same Wi-Fi, then:
1. Find your PC's LAN IP (`ipconfig` → IPv4, e.g. `192.168.1.42`).
2. In `ApiConfig.kt`, set `BASE_URL = "http://192.168.1.42:3000"`.
3. In `app/src/main/res/xml/network_security_config.xml`, add that IP as a
   `<domain>` (there's a comment showing where).

---

## 3. Run the app and test

1. In Android Studio, **Run ▶** the app on your emulator.
2. On the login screen, tap **Create an account** and sign up (or sign in with
   the demo account `demo@tapcard.app` / `password123`).
3. Create a card (scan or "Add manually") and save it.
4. **Verify sync both ways:**
   - In a browser, log in to **http://localhost:3000** with the same account →
     your card from the phone should appear there.
   - Or run `npm run db:studio` in the web repo to see the row in the database.
5. Edit or delete the card in the app → refresh the web → the change should
   reflect. Create a card on the web → reopen the app → it should show up.

### Test contacts + leads (people you meet)

The **Contacts** icon (top-right on Home, when signed in) opens your people list:

- **Add a contact** with the **+** button → it should appear in the web CRM's
  Contacts page too (same account).
- **Leads** are captured when someone fills the "share your details" form on
  your *published* web card. To simulate one: publish a card, open its public
  link (`/c/<slug>`) in a browser, submit the lead form → it should show under
  **NEW LEADS** in the app. Tap **Save** to turn it into a contact, or
  **Dismiss** to remove it.

---

## Troubleshooting

- **App shows "Couldn't connect / save"** — is `npm run dev` running? Is the
  emulator using `10.0.2.2` (not `localhost`, which on the emulator means the
  emulator itself)?
- **Physical device can't connect** — same Wi-Fi? Correct LAN IP in both
  `ApiConfig.kt` and the network security config? Windows Firewall may need to
  allow port 3000.
- **"Cleartext traffic not permitted"** — the host isn't listed in
  `network_security_config.xml`; add it.
- **DB errors on first run** — make sure `docker compose up -d db` succeeded and
  you ran `npm run setup`.

---

## Switching back to production

When you're done testing, set `ApiConfig.BASE_URL` back to
`https://tapcard.tertiaryinfotech.com` (and it'll use HTTPS automatically).
The `mobile-api` backend branch must be reviewed + deployed by the web team
before the production app can use login/sync.
