# Freunde

Native Android-App (Kotlin, Jetpack Compose, Material 3) zum Verwalten von Infos über Freunde/Bekannte
vor einem Treffen. MVVM + Repository-Pattern, Firebase (Firestore + Auth) als Backend, WorkManager für
lokale Erinnerungen.

## Projektstruktur

```
app/src/main/java/com/mhoehn/freunde/
├── data/
│   ├── model/          Person, Meeting, FixedInfo, TempInfo, Child
│   └── repository/     AuthRepository, PersonRepository, MeetingRepository, SettingsRepository
├── di/                  AppContainer (manuelles DI, kein Hilt)
├── notification/        NotificationHelper, BirthdayCheckWorker, LongTimeNoSeeWorker, ReminderScheduler
├── ui/
│   ├── navigation/       FreundeNavGraph
│   ├── screens/          list, detail, personform, meetingform, login, settings
│   ├── components/       PersonAvatar
│   └── theme/            Color, Theme, Type
├── util/                 DateUtils
├── FreundeApplication.kt
└── MainActivity.kt
```

Firestore-Datenmodell: `users/{uid}/persons/{personId}` mit Subcollection `meetings/{meetingId}`.
Jede/r Nutzer/in sieht nur die eigenen Daten (siehe `firestore.rules`).

## Firebase-Setup (nötig, bevor die App läuft)

Das Projekt enthält unter `app/google-services.json` nur eine **Platzhalter-Datei** – Anmeldung und
Sync funktionieren erst nach folgenden Schritten:

1. **Firebase-Projekt anlegen**: [console.firebase.google.com](https://console.firebase.google.com) →
   "Projekt hinzufügen".
2. **Android-App registrieren**: Paketname exakt `com.mhoehn.freunde` eingeben (steht auch in
   `app/build.gradle.kts` als `applicationId`).
3. **SHA-1-Fingerprint hinzufügen** (Pflicht für Google Sign-In): In den Projekteinstellungen der
   Android-App "Fingerprint hinzufügen". Debug-Fingerprint ermitteln mit:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   (unter Windows liegt der Debug-Keystore i.d.R. unter `%USERPROFILE%\.android\debug.keystore`).
4. **`google-services.json` herunterladen** und die Platzhalter-Datei unter `app/google-services.json`
   ersetzen.
5. **Firestore aktivieren**: Firebase Console → Build → Firestore Database → Datenbank erstellen
   (Produktionsmodus reicht, die Regeln werden im nächsten Schritt gesetzt).
6. **Security Rules deployen**: Inhalt von `firestore.rules` (im Projekt-Root) in der Firebase Console
   unter Firestore → Regeln einfügen und veröffentlichen (oder per Firebase CLI: `firebase deploy --only firestore:rules`).
7. **Google als Sign-In-Anbieter aktivieren**: Firebase Console → Build → Authentication →
   Sign-in method → Google → aktivieren, Support-E-Mail setzen.

Nach diesen Schritten enthält die echte `google-services.json` automatisch die Web-Client-ID
(`client_type: 3`), aus der das `google-services`-Gradle-Plugin die Ressource
`R.string.default_web_client_id` generiert – die App nutzt sie direkt für den Google-Sign-In-Flow
über Credential Manager, ohne dass im Code etwas angepasst werden muss.

## Projekt öffnen

1. Ordner `Freunde/` in Android Studio öffnen ("Open").
2. Da der Gradle-Wrapper-JAR nicht mitgeliefert ist (Binärdatei), bietet Android Studio beim ersten
   Öffnen an, den Wrapper zu reparieren/herunterzuladen – das bestätigen. Alternativ, falls lokal
   Gradle installiert ist: `gradle wrapper --gradle-version 8.9` im Projektordner ausführen.
3. Sync abwarten, dann `google-services.json` ersetzen (siehe oben) und erneut syncen.
4. App auf Gerät/Emulator starten.

## Bekannte Einschränkungen

- **Fotos** werden nur als lokale URI (Android Photo Picker) gespeichert, nicht über die Geräte
  synchronisiert. Für echten Foto-Sync müsste zusätzlich Firebase Storage angebunden werden.
- **Benachrichtigungen** (Geburtstage, "lange nicht gesehen") laufen über tägliche WorkManager-Jobs
  (ca. 9:00 / 10:00 Uhr) und benötigen auf Android 13+ die Notification-Runtime-Permission, die beim
  ersten App-Start abgefragt wird.
- Der Schwellwert für "lange nicht gesehen" ist über den Einstellungen-Screen konfigurierbar
  (Default: 60 Tage) und wird lokal per DataStore gespeichert (nicht geräteübergreifend synchronisiert).
