(function () {
  "use strict";

  var THRESHOLD_KEY = "freunde_threshold_days_v1";
  var NOTIFIED_DATE_KEY = "freunde_last_notified_date_v1";
  var DEFAULT_THRESHOLD = 60;

  var currentUser = null;
  var personsCache = [];
  var unsubscribePersons = null;
  var searchQuery = "";
  var thresholdDays = loadThreshold();

  var detailState = { personId: null, unsubPerson: null, unsubMeetings: null, person: null, meetings: [] };
  var personFormState = { personId: null, photoDataUrl: null, children: [] };
  var meetingFormState = { personId: null, meetingId: null };
  var confirmCallback = null;

  // ---------- kleine DOM- und Datums-Helfer ----------

  function $(id) { return document.getElementById(id); }

  function h(tag, attrs, children) {
    var elx = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (k) {
        if (k === "class") elx.className = attrs[k];
        else if (k === "text") elx.textContent = attrs[k];
        else if (k.indexOf("on") === 0 && typeof attrs[k] === "function") elx.addEventListener(k.slice(2), attrs[k]);
        else elx.setAttribute(k, attrs[k]);
      });
    }
    (children || []).forEach(function (c) {
      if (c || c === 0) elx.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    });
    return elx;
  }

  function clear(node) { while (node.firstChild) node.removeChild(node.firstChild); }

  function toDateInputValue(date) {
    if (!date) return "";
    var y = date.getFullYear();
    var m = String(date.getMonth() + 1).padStart(2, "0");
    var d = String(date.getDate()).padStart(2, "0");
    return y + "-" + m + "-" + d;
  }

  function fromDateInputValue(value) {
    if (!value) return null;
    var parts = value.split("-");
    return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
  }

  function formatDisplayDate(date) {
    if (!date) return "";
    return date.toLocaleDateString("de-DE", { day: "2-digit", month: "2-digit", year: "numeric" });
  }

  function daysSince(date) {
    var startOfToday = new Date(); startOfToday.setHours(0, 0, 0, 0);
    var startOfDate = new Date(date); startOfDate.setHours(0, 0, 0, 0);
    return Math.round((startOfToday.getTime() - startOfDate.getTime()) / 86400000);
  }

  function lastSeenLabel(lastMeetingDate) {
    if (!lastMeetingDate) return "Noch kein Treffen erfasst";
    var days = daysSince(lastMeetingDate);
    if (days <= 0) return "heute gesehen";
    if (days < 30) return "seit " + days + (days === 1 ? " Tag" : " Tagen") + " nicht gesehen";
    var months = Math.floor(days / 30);
    if (months < 12) return "seit " + months + (months === 1 ? " Monat" : " Monaten") + " nicht gesehen";
    var years = Math.floor(months / 12);
    return "seit " + years + (years === 1 ? " Jahr" : " Jahren") + " nicht gesehen";
  }

  function isBirthdayToday(birthday) {
    if (!birthday) return false;
    var now = new Date();
    return birthday.getMonth() === now.getMonth() && birthday.getDate() === now.getDate();
  }

  function isLongTimeNoSee(person) {
    if (!person.lastMeetingDate) return true;
    return daysSince(person.lastMeetingDate) >= thresholdDays;
  }

  function loadThreshold() {
    var v = parseInt(localStorage.getItem(THRESHOLD_KEY), 10);
    return isNaN(v) || v <= 0 ? DEFAULT_THRESHOLD : v;
  }

  function saveThreshold(days) {
    thresholdDays = days;
    localStorage.setItem(THRESHOLD_KEY, String(days));
  }

  function initials(name) {
    var trimmed = (name || "").trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : "?";
  }

  function avatarEl(person, size) {
    var box = h("div", { class: "avatar" + (size === "lg" ? " avatar-lg" : "") });
    if (person.photoDataUrl) {
      box.appendChild(h("img", { src: person.photoDataUrl, alt: "" }));
    } else {
      box.textContent = initials(person.name);
    }
    return box;
  }

  // ---------- Hash-Router ----------

  function parseHash() {
    var raw = location.hash.replace(/^#\/?/, "");
    var parts = raw.split("/").filter(Boolean).map(decodeURIComponent);
    if (parts.length === 0) return { screen: "list" };
    if (parts[0] === "settings") return { screen: "settings" };
    if (parts[0] === "person") {
      if (parts[1] === "new") return { screen: "person-form", personId: null };
      var personId = parts[1];
      if (!personId) return { screen: "list" };
      if (parts[2] === "edit") return { screen: "person-form", personId: personId };
      if (parts[2] === "meeting") {
        if (parts[3] === "new") return { screen: "meeting-form", personId: personId, meetingId: null };
        if (parts[3] && parts[4] === "edit") return { screen: "meeting-form", personId: personId, meetingId: parts[3] };
      }
      return { screen: "detail", personId: personId };
    }
    return { screen: "list" };
  }

  function goList() { location.hash = "#/"; }
  function goDetail(id) { location.hash = "#/person/" + encodeURIComponent(id); }
  function goNewPerson() { location.hash = "#/person/new"; }
  function goEditPerson(id) { location.hash = "#/person/" + encodeURIComponent(id) + "/edit"; }
  function goNewMeeting(personId) { location.hash = "#/person/" + encodeURIComponent(personId) + "/meeting/new"; }
  function goEditMeeting(personId, meetingId) {
    location.hash = "#/person/" + encodeURIComponent(personId) + "/meeting/" + encodeURIComponent(meetingId) + "/edit";
  }
  function goSettings() { location.hash = "#/settings"; }

  var VIEW_IDS = ["login", "list", "detail", "person-form", "meeting-form", "settings"];

  function showView(name, title) {
    VIEW_IDS.forEach(function (v) {
      $("view-" + v).classList.toggle("hidden", v !== name);
    });
    $("btn-back").classList.toggle("hidden", name === "login" || name === "list");
    $("btn-settings").classList.toggle("hidden", name !== "list");
    $("page-title").textContent = title || "Freunde";
  }

  function render() {
    if (!currentUser) {
      cleanupDetail();
      showView("login");
      return;
    }
    var route = parseHash();
    if (route.screen !== "detail") cleanupDetail();
    switch (route.screen) {
      case "detail": renderDetail(route.personId); break;
      case "person-form": renderPersonForm(route.personId); break;
      case "meeting-form": renderMeetingForm(route.personId, route.meetingId); break;
      case "settings": renderSettings(); break;
      default: renderList();
    }
  }

  window.addEventListener("hashchange", render);

  // ---------- Login ----------

  $("btn-google-signin").addEventListener("click", function () {
    $("login-error").classList.add("hidden");
    window.FreundeCloud.signInWithGoogle().catch(function (err) {
      $("login-error").textContent = err && err.message ? err.message : "Anmeldung fehlgeschlagen";
      $("login-error").classList.remove("hidden");
    });
  });

  $("btn-back").addEventListener("click", function () { history.back(); });
  $("btn-settings").addEventListener("click", goSettings);

  // ---------- Personenliste ----------

  function subscribePersonsIfNeeded() {
    if (unsubscribePersons) return;
    unsubscribePersons = window.FreundeCloud.observePersons(function (persons) {
      personsCache = persons;
      maybeNotifyToday(persons);
      if (parseHash().screen === "list") renderList();
    }, function (err) {
      console.error("observePersons", err);
    });
  }

  $("search-input").addEventListener("input", function (e) {
    searchQuery = e.target.value;
    if (parseHash().screen === "list") renderList();
  });

  $("btn-add-person").addEventListener("click", goNewPerson);

  function renderList() {
    showView("list", "Freunde");
    subscribePersonsIfNeeded();

    var filtered = searchQuery.trim()
      ? personsCache.filter(function (p) { return p.name.toLowerCase().indexOf(searchQuery.trim().toLowerCase()) !== -1; })
      : personsCache.slice();

    filtered.sort(function (a, b) {
      var ta = a.lastMeetingDate ? a.lastMeetingDate.getTime() : -Infinity;
      var tb = b.lastMeetingDate ? b.lastMeetingDate.getTime() : -Infinity;
      return ta - tb;
    });

    renderReminderBanner();

    var listEl = $("person-list");
    clear(listEl);
    $("person-list-empty").classList.toggle("hidden", filtered.length > 0);
    if (filtered.length === 0) {
      $("person-list-empty").textContent = searchQuery.trim()
        ? "Keine Treffer für \"" + searchQuery.trim() + "\""
        : "Noch keine Freunde erfasst. Tippe auf + zum Hinzufügen.";
    }

    filtered.forEach(function (person) {
      var row = h("button", { class: "person-row", onclick: function () { goDetail(person.id); } }, [
        avatarEl(person),
        h("div", { class: "person-row-info" }, [
          h("div", { class: "person-row-name", text: person.name }),
          h("div", { class: "person-row-sub", text: lastSeenLabel(person.lastMeetingDate) })
        ]),
        isLongTimeNoSee(person) ? h("div", { class: "badge", text: "!" }) : null
      ]);
      listEl.appendChild(row);
    });
  }

  function renderReminderBanner() {
    var banner = $("reminder-banner");
    var birthdayPeople = personsCache.filter(function (p) { return isBirthdayToday(p.birthday); });
    if (birthdayPeople.length === 0) {
      banner.classList.add("hidden");
      return;
    }
    clear(banner);
    banner.appendChild(h("p", { text: "🎂 Heute Geburtstag: " + birthdayPeople.map(function (p) { return p.name; }).join(", ") }));
    banner.classList.remove("hidden");
  }

  // ---------- Personendetail ----------

  function cleanupDetail() {
    if (detailState.unsubPerson) detailState.unsubPerson();
    if (detailState.unsubMeetings) detailState.unsubMeetings();
    detailState.unsubPerson = null;
    detailState.unsubMeetings = null;
    detailState.personId = null;
    detailState.person = null;
    detailState.meetings = [];
  }

  function renderDetail(personId) {
    if (detailState.personId !== personId) {
      cleanupDetail();
      detailState.personId = personId;
      detailState.unsubPerson = window.FreundeCloud.observePerson(personId, function (person) {
        if (!person) { goList(); return; }
        detailState.person = person;
        if (parseHash().screen === "detail") renderDetailContent();
      });
      detailState.unsubMeetings = window.FreundeCloud.observeMeetings(personId, function (meetings) {
        detailState.meetings = meetings;
        if (parseHash().screen === "detail") renderDetailContent();
      });
    }
    showView("detail", detailState.person ? detailState.person.name : "");
    if (detailState.person) renderDetailContent();
  }

  function renderDetailContent() {
    var person = detailState.person;
    if (!person) return;
    $("page-title").textContent = person.name;
    var avatarHost = $("detail-avatar");
    clear(avatarHost);
    avatarHost.className = "avatar avatar-lg";
    if (person.photoDataUrl) {
      avatarHost.appendChild(h("img", { src: person.photoDataUrl, alt: "" }));
    } else {
      avatarHost.textContent = initials(person.name);
    }

    $("detail-name").textContent = person.name;
    $("detail-last-seen").textContent = lastSeenLabel(person.lastMeetingDate);

    var birthdayEl = $("detail-birthday");
    if (person.birthday) {
      birthdayEl.textContent = "🎂 " + formatDisplayDate(person.birthday);
      birthdayEl.classList.remove("hidden");
    } else {
      birthdayEl.classList.add("hidden");
    }

    renderFixedInfo(person);
    renderTempInfo(person);
    renderMeetingsList();
  }

  function renderFixedInfo(person) {
    var card = $("fixed-info-card");
    var content = $("fixed-info-content");
    clear(content);
    var hasContent = person.partnerName || (person.children && person.children.length) || person.otherFixedInfo;
    card.classList.toggle("hidden", !hasContent);
    if (!hasContent) return;

    if (person.partnerName) {
      content.appendChild(h("p", { text: "Partner/in: " + person.partnerName }));
    }
    (person.children || []).forEach(function (child) {
      var label = "Kind: " + child.name + (child.birthYear ? " (geb. " + child.birthYear + ")" : "");
      content.appendChild(h("p", { text: label }));
    });
    if (person.otherFixedInfo) {
      content.appendChild(h("p", { text: person.otherFixedInfo }));
    }
  }

  var tempEditing = false;

  function renderTempInfo(person) {
    var view = $("temp-info-view");
    var edit = $("temp-info-edit");
    clear(view);

    if (!tempEditing) {
      edit.classList.add("hidden");
      view.classList.remove("hidden");
      var rows = [];
      if (person.currentJob) rows.push("Job: " + person.currentJob);
      if (person.hobbies) rows.push("Hobbys: " + person.hobbies);
      if (person.vacation) rows.push("Urlaub: " + person.vacation);
      if (person.tempNotes) rows.push(person.tempNotes);
      if (rows.length === 0) {
        view.appendChild(h("p", { class: "muted", text: "Noch keine aktuellen Infos hinterlegt." }));
      } else {
        rows.forEach(function (r) { view.appendChild(h("p", { text: r })); });
      }
    } else {
      view.classList.add("hidden");
      edit.classList.remove("hidden");
      $("temp-edit-job").value = person.currentJob || "";
      $("temp-edit-hobbies").value = person.hobbies || "";
      $("temp-edit-vacation").value = person.vacation || "";
      $("temp-edit-notes").value = person.tempNotes || "";
    }
  }

  $("btn-toggle-temp-edit").addEventListener("click", function () {
    tempEditing = !tempEditing;
    if (detailState.person) renderTempInfo(detailState.person);
  });

  $("btn-save-temp-info").addEventListener("click", function () {
    if (!detailState.personId) return;
    window.FreundeCloud.updateTempInfo(detailState.personId, {
      currentJob: $("temp-edit-job").value.trim(),
      hobbies: $("temp-edit-hobbies").value.trim(),
      vacation: $("temp-edit-vacation").value.trim(),
      tempNotes: $("temp-edit-notes").value.trim()
    }).then(function () {
      tempEditing = false;
    }).catch(showToast);
  });

  function renderMeetingsList() {
    var listEl = $("meetings-list");
    clear(listEl);
    var meetings = detailState.meetings;
    $("meetings-empty").classList.toggle("hidden", meetings.length > 0);

    meetings.forEach(function (meeting) {
      var main = h("div", { class: "meeting-card-main" }, [
        h("div", { class: "meeting-date-row" }, [formatDisplayDate(meeting.date)]),
      ]);
      if (meeting.location) main.appendChild(h("div", { class: "meeting-location", text: "📍 " + meeting.location }));
      if (meeting.notes) main.appendChild(h("div", { class: "meeting-notes", text: meeting.notes }));

      var card = h("div", { class: "meeting-card" }, [
        main,
        h("button", {
          class: "meeting-delete-btn",
          "aria-label": "Treffen löschen",
          onclick: function (e) {
            e.stopPropagation();
            askConfirm("Dieses Treffen wirklich löschen?", function () {
              window.FreundeCloud.deleteMeeting(detailState.personId, meeting.id).catch(showToast);
            });
          }
        }, ["🗑️"])
      ]);
      card.addEventListener("click", function () { goEditMeeting(detailState.personId, meeting.id); });
      listEl.appendChild(card);
    });
  }

  $("btn-add-meeting").addEventListener("click", function () {
    if (detailState.personId) goNewMeeting(detailState.personId);
  });

  $("btn-edit-person").addEventListener("click", function () {
    if (detailState.personId) goEditPerson(detailState.personId);
  });

  $("btn-delete-person").addEventListener("click", function () {
    if (!detailState.personId || !detailState.person) return;
    var name = detailState.person.name;
    askConfirm(name + " und alle erfassten Treffen werden dauerhaft gelöscht. Fortfahren?", function () {
      window.FreundeCloud.deletePerson(detailState.personId).then(function () {
        goList();
      }).catch(showToast);
    });
  });

  // ---------- Person anlegen/bearbeiten ----------

  function renderPersonForm(personId) {
    tempEditing = false;
    personFormState.personId = personId;
    personFormState.photoDataUrl = null;
    personFormState.children = [];
    $("person-form-name-error").classList.add("hidden");
    $("person-form").reset();
    clear($("children-editor"));
    updatePersonFormAvatar(null, "");

    showView("person-form", personId ? "Person bearbeiten" : "Person anlegen");

    if (personId) {
      window.FreundeCloud.getPerson(personId).then(function (person) {
        if (!person) { goList(); return; }
        $("person-form-name").value = person.name;
        $("person-form-birthday").value = toDateInputValue(person.birthday);
        $("person-form-partner").value = person.partnerName || "";
        $("person-form-other-fixed").value = person.otherFixedInfo || "";
        $("person-form-job").value = person.currentJob || "";
        $("person-form-hobbies").value = person.hobbies || "";
        $("person-form-vacation").value = person.vacation || "";
        $("person-form-notes").value = person.tempNotes || "";
        personFormState.photoDataUrl = person.photoDataUrl || null;
        personFormState.children = (person.children || []).slice();
        updatePersonFormAvatar(person.photoDataUrl, person.name);
        renderChildrenEditor();
      }).catch(showToast);
    } else {
      renderChildrenEditor();
    }
  }

  function updatePersonFormAvatar(photoDataUrl, name) {
    var host = $("person-form-avatar");
    clear(host);
    if (photoDataUrl) {
      host.appendChild(h("img", { src: photoDataUrl, alt: "" }));
    } else {
      host.textContent = initials(name || $("person-form-name").value);
    }
  }

  $("btn-pick-photo").addEventListener("click", function () { $("person-form-photo-input").click(); });

  $("person-form-photo-input").addEventListener("change", function (e) {
    var file = e.target.files && e.target.files[0];
    if (!file) return;
    resizeImageToDataUrl(file, 200).then(function (dataUrl) {
      personFormState.photoDataUrl = dataUrl;
      updatePersonFormAvatar(dataUrl, "");
    }).catch(showToast);
  });

  $("person-form-name").addEventListener("input", function () {
    if (!personFormState.photoDataUrl) updatePersonFormAvatar(null, $("person-form-name").value);
  });

  function resizeImageToDataUrl(file, maxSize) {
    return new Promise(function (resolve, reject) {
      var img = new Image();
      var reader = new FileReader();
      reader.onerror = reject;
      reader.onload = function () {
        img.onerror = reject;
        img.onload = function () {
          var scale = Math.min(1, maxSize / Math.max(img.width, img.height));
          var w = Math.round(img.width * scale), hgt = Math.round(img.height * scale);
          var canvas = document.createElement("canvas");
          canvas.width = w; canvas.height = hgt;
          canvas.getContext("2d").drawImage(img, 0, 0, w, hgt);
          resolve(canvas.toDataURL("image/jpeg", 0.85));
        };
        img.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }

  function renderChildrenEditor() {
    var host = $("children-editor");
    clear(host);
    personFormState.children.forEach(function (child, index) {
      var nameInput = h("input", {
        class: "text-input child-name", type: "text", placeholder: "Name", value: child.name || ""
      });
      nameInput.addEventListener("input", function () { personFormState.children[index].name = nameInput.value; });

      var yearInput = h("input", {
        class: "text-input child-year", type: "number", placeholder: "Jahrgang",
        value: child.birthYear != null ? String(child.birthYear) : ""
      });
      yearInput.addEventListener("input", function () {
        var v = parseInt(yearInput.value, 10);
        personFormState.children[index].birthYear = isNaN(v) ? null : v;
      });

      var removeBtn = h("button", {
        class: "child-remove-btn", type: "button", "aria-label": "Kind entfernen",
        onclick: function () { personFormState.children.splice(index, 1); renderChildrenEditor(); }
      }, ["✕"]);

      host.appendChild(h("div", { class: "child-row" }, [nameInput, yearInput, removeBtn]));
    });
  }

  $("btn-add-child").addEventListener("click", function () {
    personFormState.children.push({ name: "", birthYear: null });
    renderChildrenEditor();
  });

  $("person-form").addEventListener("submit", function (e) {
    e.preventDefault();
    var name = $("person-form-name").value.trim();
    if (!name) {
      $("person-form-name-error").classList.remove("hidden");
      return;
    }
    var saveBtn = $("btn-save-person");
    saveBtn.disabled = true;

    var person = {
      id: personFormState.personId,
      name: name,
      photoDataUrl: personFormState.photoDataUrl,
      partnerName: $("person-form-partner").value.trim(),
      children: personFormState.children.filter(function (c) { return c.name && c.name.trim(); }),
      otherFixedInfo: $("person-form-other-fixed").value.trim(),
      currentJob: $("person-form-job").value.trim(),
      hobbies: $("person-form-hobbies").value.trim(),
      vacation: $("person-form-vacation").value.trim(),
      tempNotes: $("person-form-notes").value.trim(),
      birthday: fromDateInputValue($("person-form-birthday").value)
    };

    window.FreundeCloud.savePerson(person).then(function (id) {
      saveBtn.disabled = false;
      goDetail(id);
    }).catch(function (err) {
      saveBtn.disabled = false;
      showToast(err);
    });
  });

  // ---------- Treffen anlegen/bearbeiten ----------

  function renderMeetingForm(personId, meetingId) {
    meetingFormState.personId = personId;
    meetingFormState.meetingId = meetingId;
    $("meeting-form").reset();
    $("meeting-form-date").value = toDateInputValue(new Date());

    showView("meeting-form", meetingId ? "Treffen bearbeiten" : "Neues Treffen");

    if (meetingId) {
      window.FreundeCloud.getMeeting(personId, meetingId).then(function (meeting) {
        if (!meeting) { goDetail(personId); return; }
        $("meeting-form-date").value = toDateInputValue(meeting.date);
        $("meeting-form-location").value = meeting.location || "";
        $("meeting-form-notes").value = meeting.notes || "";
      }).catch(showToast);
    }
  }

  $("meeting-form").addEventListener("submit", function (e) {
    e.preventDefault();
    var saveBtn = $("btn-save-meeting");
    saveBtn.disabled = true;
    var meeting = {
      id: meetingFormState.meetingId,
      date: fromDateInputValue($("meeting-form-date").value) || new Date(),
      location: $("meeting-form-location").value.trim(),
      notes: $("meeting-form-notes").value.trim()
    };
    window.FreundeCloud.saveMeeting(meetingFormState.personId, meeting).then(function () {
      saveBtn.disabled = false;
      goDetail(meetingFormState.personId);
    }).catch(function (err) {
      saveBtn.disabled = false;
      showToast(err);
    });
  });

  // ---------- Einstellungen ----------

  function renderSettings() {
    showView("settings", "Einstellungen");
    $("settings-threshold").value = String(thresholdDays);
  }

  $("settings-threshold").addEventListener("input", function () {
    var v = parseInt($("settings-threshold").value, 10);
    if (!isNaN(v) && v > 0) saveThreshold(v);
  });

  $("btn-enable-notifications").addEventListener("click", function () {
    if (!("Notification" in window)) {
      showToast("Dein Browser unterstützt keine Benachrichtigungen.");
      return;
    }
    Notification.requestPermission().then(function (permission) {
      showToast(permission === "granted" ? "Benachrichtigungen aktiviert." : "Keine Berechtigung erteilt.");
    });
  });

  $("btn-sign-out").addEventListener("click", function () {
    window.FreundeCloud.signOut();
  });

  // ---------- Erinnerungen (nur beim Öffnen der App, kein echter Hintergrund-Push) ----------

  function maybeNotifyToday(persons) {
    if (!("Notification" in window) || Notification.permission !== "granted") return;
    var todayKey = toDateInputValue(new Date());
    if (localStorage.getItem(NOTIFIED_DATE_KEY) === todayKey) return;
    localStorage.setItem(NOTIFIED_DATE_KEY, todayKey);

    persons.filter(function (p) { return isBirthdayToday(p.birthday); }).forEach(function (p) {
      try { new Notification(p.name + " hat heute Geburtstag 🎉", { body: "Denk dran zu gratulieren!" }); } catch (e) {}
    });

    var longTimeNoSee = persons.filter(isLongTimeNoSee);
    if (longTimeNoSee.length > 0) {
      try {
        new Notification("Lange nicht gesehen", {
          body: longTimeNoSee.length === 1
            ? longTimeNoSee[0].name + " hast du lange nicht gesehen."
            : longTimeNoSee.length + " Freunde hast du lange nicht gesehen."
        });
      } catch (e) {}
    }
  }

  // ---------- Bestätigungsdialog & Toast ----------

  function askConfirm(text, onConfirm) {
    $("confirm-dialog-text").textContent = text;
    confirmCallback = onConfirm;
    $("confirm-dialog").classList.remove("hidden");
  }

  $("confirm-dialog-cancel").addEventListener("click", function () {
    $("confirm-dialog").classList.add("hidden");
    confirmCallback = null;
  });

  $("confirm-dialog-ok").addEventListener("click", function () {
    $("confirm-dialog").classList.add("hidden");
    var cb = confirmCallback;
    confirmCallback = null;
    if (cb) cb();
  });

  var toastTimer = null;
  function showToast(message) {
    var text = message && message.message ? message.message : (typeof message === "string" ? message : "Etwas ist schiefgelaufen.");
    $("toast-text").textContent = text;
    $("toast").classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { $("toast").classList.add("hidden"); }, 3000);
  }

  // ---------- Start ----------

  function start() {
    window.FreundeCloud.onAuthChange(function (user) {
      var wasLoggedIn = !!currentUser;
      currentUser = user;
      if (user && !wasLoggedIn) {
        if (!location.hash) location.hash = "#/";
      }
      if (!user) {
        if (unsubscribePersons) { unsubscribePersons(); unsubscribePersons = null; }
        personsCache = [];
      }
      render();
    });

    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("service-worker.js").catch(function () {});
    }
  }

  if (window.FreundeCloud) {
    start();
  } else {
    window.addEventListener("freunde-cloud-ready", start, { once: true });
  }
})();
