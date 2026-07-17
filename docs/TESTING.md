# Tapcard — End-to-End Test Checklist

Local test setup: backend `tapcard-web` on `http://192.168.3.43:3000`, app pointed at the same IP.
Tick items as you verify them on-device / in-browser.

---

## 🔴 Priority — changed this session (test first)

- [ ] **App: Sign up** (Get started) → Name + Email + Password, eye toggle works, account created
- [ ] **App: Log in → Password tab** → email + password logs in
- [ ] **App: Log in → Email OTP tab** → email → 6-digit code arrives → verify; Resend works
- [ ] **App: Sign out** → returns cleanly to the welcome screen
- [ ] **App: card editor** → NO Intro video / Gallery / Links sections; nothing looks broken
- [ ] **Web: landing page** → "Sign in" + "Create free card" buttons visible top-right
- [ ] **Web: login** → password eye toggle works (open eye = visible)
- [ ] **Web: Sign out** → lands on home, NOT the `0.0.0.0` error

---

## 📱 App (tapcardapp_android)

### Auth
- [ ] Sign up (password) creates an account and lands in the app
- [ ] Login — Password tab
- [ ] Login — Email OTP tab (code + resend)
- [ ] Switch between Sign up / Log in links
- [ ] Validation: bad email, short password, wrong password, wrong code all show errors
- [ ] Sign out → welcome; log back in

### Cards
- [ ] Create a card manually; edit an existing card
- [ ] All fields save: name, title, company, department, tagline, bio, about, phone, office phone, WhatsApp, email, website, address, 7 socials
- [ ] Profile photo / company logo / cover banner (pick from gallery + camera)
- [ ] Theme picker (6 themes) + accent color
- [ ] "Write with AI" for bio + about
- [ ] Delete a card
- [ ] Multiple cards swipe on Home

### Scan
- [ ] Scan a paper business card (OCR)
- [ ] Scan a QR code (vCard / link)
- [ ] Scan a Tapcard web-card QR → pulls full details
- [ ] Enter manually

### Share
- [ ] Share sheet: QR shows, copy link, share link, Send via SMS
- [ ] Publish to web → get a live link
- [ ] Save to contacts (vCard); Call / WhatsApp / Email / Website buttons
- [ ] Email signature (copy) · Virtual background (save/share)
- [ ] Home-screen widget (add from launcher)
- [ ] NFC tap-to-share (needs a 2nd phone — skip if unavailable)

### Contacts / Planner / Analytics / Settings
- [ ] Leads inbox: save-as-contact / dismiss
- [ ] Contacts: add, delete, call/email, search
- [ ] Planner: add/edit/toggle/delete tasks
- [ ] Planner: add/edit/delete appointments; add to device calendar
- [ ] Analytics: stats load
- [ ] Settings: account info, Help/Privacy links, logout, delete account

---

## 🌐 Web (tapcard-web)

### Public / marketing
- [ ] Landing loads (hero, features, pricing); no broken/black hero box
- [ ] Theme toggle (light/dark)
- [ ] Public card `/c/slug`: renders, action buttons, vCard download, QR
- [ ] Lead form on a public card submits

### Auth
- [ ] Register a new account
- [ ] Login — password tab (eye toggle)
- [ ] Login — Email OTP tab
- [ ] Forgot password → OTP link
- [ ] Sign out (no `0.0.0.0`)

### Dashboard
- [ ] Dashboard stat tiles load
- [ ] Card builder: all fields, autosave, publish toggle, live preview
- [ ] Share panel: QR (PNG/SVG, 5 target modes), copy link
- [ ] Confirm NO gallery / links / intro-video editors
- [ ] Delete a card (confirm dialog)

### CRM
- [ ] Contacts: add / edit / delete, tags, CSV export
- [ ] Leads: convert to contact, AI score, delete
- [ ] Pipeline: drag deals between stages, add/delete deal
- [ ] Planner: tasks + appointments, calendar, download .ics
- [ ] Analytics page

---

## 🔗 Cross-platform sync (the important glue)

- [ ] Create a card on the **app** → appears on the **web** dashboard
- [ ] Create / edit a card on **web** → shows on the **app**
- [ ] **Publish on app** → open that link in a browser → card renders
- [ ] Submit a **lead on a web public card** → appears in the **app** Contacts/Leads
- [ ] Add a **task/appointment on app** → shows in **web** Planner (and vice-versa)
