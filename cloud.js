import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  signInWithRedirect,
  signInWithPopup,
  getRedirectResult,
  onAuthStateChanged,
  signOut as firebaseSignOut
} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";
import {
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager,
  doc,
  addDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  getDocs,
  collection,
  onSnapshot,
  query,
  orderBy,
  limit,
  writeBatch,
  serverTimestamp,
  Timestamp
} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

(function () {
  "use strict";

  var app = initializeApp(window.FREUNDE_FIREBASE_CONFIG);
  var auth = getAuth(app);
  var db = initializeFirestore(app, {
    localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() })
  });

  var authCallbacks = [];
  var currentUser = null;

  onAuthStateChanged(auth, function (user) {
    currentUser = user;
    authCallbacks.forEach(function (cb) { cb(user); });
  });

  function isMobile() {
    return /Mobi|Android|iPhone|iPad/i.test(navigator.userAgent);
  }

  function signInWithGoogle() {
    var provider = new GoogleAuthProvider();
    if (isMobile()) {
      return signInWithRedirect(auth, provider);
    }
    return signInWithPopup(auth, provider);
  }

  // Ergebnis eines Redirect-Sign-Ins (mobil) einsammeln - Fehler hier sind unkritisch,
  // z.B. wenn einfach kein Redirect-Login im Gange war.
  getRedirectResult(auth).catch(function () {});

  function personsCol(uid) {
    return collection(db, "users", uid, "persons");
  }
  function personDoc(uid, personId) {
    return doc(db, "users", uid, "persons", personId);
  }
  function meetingsCol(uid, personId) {
    return collection(db, "users", uid, "persons", personId, "meetings");
  }
  function meetingDoc(uid, personId, meetingId) {
    return doc(db, "users", uid, "persons", personId, "meetings", meetingId);
  }

  function dateToTimestamp(date) {
    return date ? Timestamp.fromDate(date) : null;
  }
  function timestampToDate(ts) {
    return ts ? ts.toDate() : null;
  }

  function personFromSnapshot(docSnap) {
    var data = docSnap.data();
    return {
      id: docSnap.id,
      name: data.name || "",
      photoDataUrl: data.photoDataUrl || null,
      partnerName: data.partnerName || "",
      children: data.children || [],
      otherFixedInfo: data.otherFixedInfo || "",
      currentJob: data.currentJob || "",
      hobbies: data.hobbies || "",
      vacation: data.vacation || "",
      tempNotes: data.tempNotes || "",
      birthday: timestampToDate(data.birthday),
      lastMeetingDate: timestampToDate(data.lastMeetingDate)
    };
  }

  function meetingFromSnapshot(docSnap) {
    var data = docSnap.data();
    return {
      id: docSnap.id,
      date: timestampToDate(data.date) || new Date(),
      location: data.location || "",
      notes: data.notes || ""
    };
  }

  function requireUid() {
    if (!currentUser) throw new Error("Nicht angemeldet");
    return currentUser.uid;
  }

  var FreundeCloud = {
    onAuthChange: function (cb) {
      authCallbacks.push(cb);
      cb(currentUser);
      return function () {
        authCallbacks = authCallbacks.filter(function (c) { return c !== cb; });
      };
    },
    getCurrentUser: function () { return currentUser; },
    signInWithGoogle: signInWithGoogle,
    signOut: function () { return firebaseSignOut(auth); },

    observePersons: function (onData, onError) {
      var uid = requireUid();
      return onSnapshot(personsCol(uid), function (snap) {
        onData(snap.docs.map(personFromSnapshot));
      }, onError);
    },

    observePerson: function (personId, onData, onError) {
      var uid = requireUid();
      return onSnapshot(personDoc(uid, personId), function (snap) {
        onData(snap.exists() ? personFromSnapshot(snap) : null);
      }, onError);
    },

    getPerson: function (personId) {
      var uid = requireUid();
      return getDoc(personDoc(uid, personId)).then(function (snap) {
        return snap.exists() ? personFromSnapshot(snap) : null;
      });
    },

    getMeeting: function (personId, meetingId) {
      var uid = requireUid();
      return getDoc(meetingDoc(uid, personId, meetingId)).then(function (snap) {
        return snap.exists() ? meetingFromSnapshot(snap) : null;
      });
    },

    observeMeetings: function (personId, onData, onError) {
      var uid = requireUid();
      var q = query(meetingsCol(uid, personId), orderBy("date", "desc"));
      return onSnapshot(q, function (snap) {
        onData(snap.docs.map(meetingFromSnapshot));
      }, onError);
    },

    savePerson: function (person) {
      var uid = requireUid();
      var payload = {
        name: person.name,
        photoDataUrl: person.photoDataUrl || null,
        partnerName: person.partnerName || "",
        children: person.children || [],
        otherFixedInfo: person.otherFixedInfo || "",
        currentJob: person.currentJob || "",
        hobbies: person.hobbies || "",
        vacation: person.vacation || "",
        tempNotes: person.tempNotes || "",
        birthday: dateToTimestamp(person.birthday)
      };
      if (person.id) {
        return updateDoc(personDoc(uid, person.id), payload).then(function () { return person.id; });
      }
      payload.lastMeetingDate = null;
      payload.createdAt = serverTimestamp();
      return addDoc(personsCol(uid), payload).then(function (ref) { return ref.id; });
    },

    updateTempInfo: function (personId, tempInfo) {
      var uid = requireUid();
      return updateDoc(personDoc(uid, personId), {
        currentJob: tempInfo.currentJob || "",
        hobbies: tempInfo.hobbies || "",
        vacation: tempInfo.vacation || "",
        tempNotes: tempInfo.tempNotes || ""
      });
    },

    deletePerson: function (personId) {
      var uid = requireUid();
      return getDocs(meetingsCol(uid, personId)).then(function (snap) {
        var batch = writeBatch(db);
        snap.docs.forEach(function (d) { batch.delete(d.ref); });
        batch.delete(personDoc(uid, personId));
        return batch.commit();
      });
    },

    saveMeeting: function (personId, meeting) {
      var uid = requireUid();
      var payload = {
        date: dateToTimestamp(meeting.date),
        location: meeting.location || "",
        notes: meeting.notes || ""
      };
      var savePromise;
      if (meeting.id) {
        savePromise = updateDoc(meetingDoc(uid, personId, meeting.id), payload);
      } else {
        payload.createdAt = serverTimestamp();
        savePromise = addDoc(meetingsCol(uid, personId), payload);
      }
      return savePromise.then(function () { return refreshLastMeetingDate(uid, personId); });
    },

    deleteMeeting: function (personId, meetingId) {
      var uid = requireUid();
      return deleteDoc(meetingDoc(uid, personId, meetingId)).then(function () {
        return refreshLastMeetingDate(uid, personId);
      });
    }
  };

  function refreshLastMeetingDate(uid, personId) {
    var q = query(meetingsCol(uid, personId), orderBy("date", "desc"), limit(1));
    return getDocs(q).then(function (snap) {
      var latest = snap.empty ? null : timestampToDate(snap.docs[0].data().date);
      return updateDoc(personDoc(uid, personId), { lastMeetingDate: dateToTimestamp(latest) });
    });
  }

  window.FreundeCloud = FreundeCloud;
  window.dispatchEvent(new Event("freunde-cloud-ready"));
})();
