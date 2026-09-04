# Freunde

Web-App (PWA) zum Verwalten von Infos über Freunde/Bekannte, damit du vor einem Treffen schnell
nachschauen kannst, was zuletzt besprochen wurde. Läuft im Browser bzw. als "installierte" App auf
dem Handy - kein Android Studio, kein Kompilieren nötig. Backend ist Firebase (Firestore + Google-Anmeldung).

Aufgebaut nach dem gleichen Prinzip wie die [Liegestütze-Tracker-App](../liegestuetze-tracker).

## Dateien

```
index.html          Seitenstruktur (alle "Screens" liegen im DOM, werden per JS ein-/ausgeblendet)
style.css            Design (hell/dunkel automatisch je nach Systemeinstellung)
app.js                UI-Logik, Hash-Routing (#/, #/person/xyz, ...), Formulare, Erinnerungen
cloud.js              Firebase-Anbindung (Auth + Firestore), als window.FreundeCloud
firebase-config.js    Platzhalter-Zugangsdaten - siehe Setup unten
manifest.json          PWA-Manifest ("Zum Homescreen hinzufügen")
service-worker.js      Offline-Caching der statischen Dateien
firestore.rules        Sicherheitsregeln: jede/r Nutzer/in sieht nur eigene Daten
generate-icons.ps1     Erzeugt die App-Icons (icons/*.png)
serve.ps1              Lokaler Test-Server (siehe unten)
```

Firestore-Datenmodell: `users/{uid}/persons/{personId}` mit Subcollection `meetings/{meetingId}`,
genau wie ursprünglich für die native Version geplant.

## Firebase-Setup (einmalig nötig)

Ohne echte Firebase-Zugangsdaten zeigt die App nur den Login-Screen, Anmeldung/Sync funktionieren
nicht. So richtest du es ein:

1. **Firebase-Projekt anlegen**: [console.firebase.google.com](https://console.firebase.google.com) →
   "Projekt hinzufügen" (z.B. "freunde-app").
2. **Web-App registrieren**: Im Projekt auf das `</>`-Symbol ("Web-App hinzufügen") klicken,
   einen Namen vergeben (Firebase Hosting kannst du dabei abwählen, wir nutzen GitHub Pages).
3. **Zugangsdaten kopieren**: Firebase zeigt dir ein Objekt mit `apiKey`, `authDomain`,
   `projectId` usw. Diese Werte in `firebase-config.js` eintragen (die Platzhalter ersetzen).
   Diese Werte sind **kein Geheimnis** - sie stehen bei jeder Firebase-Web-App offen im Code,
   die eigentliche Absicherung passiert über die Firestore Security Rules.
4. **Firestore aktivieren**: Firebase Console → Build → Firestore Database → Datenbank erstellen.
5. **Security Rules setzen**: Unter Firestore → Regeln den Inhalt von `firestore.rules` einfügen
   und veröffentlichen.
6. **Google-Anmeldung aktivieren**: Firebase Console → Build → Authentication → Sign-in method →
   Google → aktivieren, Support-E-Mail setzen.
7. **Domain freischalten**: Firebase Console → Authentication → Settings → Authorized domains →
   `<dein-github-username>.github.io` hinzufügen (sonst blockiert Google die Anmeldung auf der
   GitHub-Pages-URL).

## Auf GitHub veröffentlichen (GitHub Pages)

Sobald der Ordner `Freunde/` im Wurzelverzeichnis des Repos liegt (so wie hier):

1. Im Repo auf GitHub: Settings → Pages.
2. Unter "Build and deployment" → Source: "Deploy from a branch".
3. Branch: `main`, Ordner: `/ (root)` → Save.
4. Nach ein bis zwei Minuten ist die App live unter `https://<dein-github-username>.github.io/<repo-name>/`.

Jeder weitere `git push` auf `main` aktualisiert die Seite automatisch - kein Build-Schritt,
kein Terminal auf deiner Seite nötig.

## Lokal testen (optional)

```powershell
powershell -ExecutionPolicy Bypass -File serve.ps1
```

Startet einen einfachen lokalen Server auf `http://localhost:8081`. Google-Anmeldung funktioniert
lokal nur, wenn `localhost` ebenfalls unter "Authorized domains" in Firebase Authentication steht
(das ist dort standardmäßig schon der Fall).

## Bekannte Einschränkungen

- **Fotos** werden als kleines, komprimiertes Bild direkt im Firestore-Dokument gespeichert
  (kein separates Firebase Storage nötig) - dadurch bewusst niedrig aufgelöst.
- **Erinnerungen** (Geburtstage, "lange nicht gesehen") erscheinen als Banner in der App und
  optional als Browser-Benachrichtigung, aber nur wenn du die App an dem Tag auch öffnest - echte
  Hintergrund-Benachrichtigungen wie bei einer nativen App bräuchten einen zusätzlichen Server
  (z.B. Firebase Cloud Functions mit Zeitplan), was hier bewusst nicht eingerichtet ist.
- Der "Lange nicht gesehen"-Schwellwert (Default: 60 Tage) wird pro Gerät im Browser gespeichert
  (localStorage), nicht über Firestore synchronisiert.
