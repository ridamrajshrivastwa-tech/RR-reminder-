const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any user backup collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").set({
      userId: ALICE_UID,
      title: "Hack",
      description: "",
      triggerAtMillis: Date.now() + 60000,
      repeatInterval: "NONE",
      customRepeatDays: 0,
      priority: "HIGH",
      category: "Work",
      colorHex: 0xFF3B82F6,
      completed: false,
      archived: false,
      snoozedUntilMillis: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, update, list, and delete reminders; ghost fields & cross-user access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").set({
      userId: ALICE_UID,
      title: "Study Mathematics",
      description: "Chapter 4 calculus",
      triggerAtMillis: Date.now() + 3600000,
      repeatInterval: "NONE",
      customRepeatDays: 0,
      priority: "HIGH",
      category: "Study",
      colorHex: 0xFF3B82F6,
      completed: false,
      archived: false,
      snoozedUntilMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // List own collection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject createdAt tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost @DocumentId 'id' field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_2").set({
      id: "rem_2",
      userId: ALICE_UID,
      title: "Bad ID",
      description: "",
      triggerAtMillis: Date.now() + 3600000,
      repeatInterval: "NONE",
      customRepeatDays: 0,
      priority: "LOW",
      category: "Personal",
      colorHex: 0xFF3B82F6,
      completed: false,
      archived: false,
      snoozedUntilMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject cross-user read and write
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").delete()
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").delete()
  );
});

test("Owner can backup alarms, events, and immutable backup_history records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alarms").doc("alarm_1").set({
      userId: ALICE_UID,
      label: "Morning Run",
      hour: 6,
      minute: 30,
      enabled: true,
      vibrate: true,
      gradualVolume: true,
      snoozeMinutes: 10,
      toneName: "Dawn Chime",
      weekdaysCsv: "1,2,3,4,5",
      bedtimeAlarm: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("event_1").set({
      userId: ALICE_UID,
      title: "Team Sync",
      notes: "Quarterly roadmap",
      dateEpochDay: 20779,
      startMinutes: 600,
      endMinutes: 660,
      colorHex: 0xFF3B82F6,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backup_history").doc("bkp_1").set({
      userId: ALICE_UID,
      reminderCount: 4,
      alarmCount: 2,
      eventCount: 1,
      status: "SUCCESS",
      deviceLabel: "Android Device",
      createdAt: now,
    })
  );

  // backup_history is immutable (update forbidden)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("backup_history").doc("bkp_1").update({
      reminderCount: 99,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("backup_history").doc("bkp_future").set({
      userId: ALICE_UID,
      reminderCount: 1,
      alarmCount: 1,
      eventCount: 0,
      status: "SUCCESS",
      deviceLabel: "Android Device",
      createdAt: new Date(Date.now() + 120000),
    })
  );
});
