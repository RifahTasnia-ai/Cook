const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_turbo_123";
const BOB_UID = "bob_turbo_456";
const DATE_KEY = "2026-11-20";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function pastTime(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write any Turbo Mode subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc(DATE_KEY).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Unauthorized task",
      tier: "S",
      orderIndex: 0,
      completed: false,
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("Tasks: owner CRUD, partial update, cross-user denial, and schema/ghost-field rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastTime(10000);

  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_s1");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "Finalize core API architecture spec",
      tier: "S",
      orderIndex: 0,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching Kotlin TurboRepository.observeTasks
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").orderBy("orderIndex").get()
  );

  // Partial update preserving userId & createdAt
  await assertSucceeds(
    taskRef.update({
      completed: true,
      updatedAt: pastTime(1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    taskRef.update({
      createdAt: pastTime(500),
      updatedAt: pastTime(500),
    })
  );

  // Reject ghost field / @DocumentId `id` injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_ghost").set({
      id: "task_ghost",
      userId: ALICE_UID,
      title: "Bad payload",
      tier: "A",
      orderIndex: 1,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid tier
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad_tier").set({
      userId: ALICE_UID,
      title: "Invalid tier",
      tier: "Z",
      orderIndex: 1,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_future").set({
      userId: ALICE_UID,
      title: "Future timestamp",
      tier: "A",
      orderIndex: 1,
      completed: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_s1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_s1").update({
      title: "Hijacked",
      updatedAt: pastTime(500),
    })
  );
  await assertSucceeds(taskRef.delete());
});

test("DailyLog: owner create, partial update, immutable dateKey, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastTime(10000);
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc(DATE_KEY);

  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      dateKey: DATE_KEY,
      dayState: "WAKE_UP",
      totalScore: 38.0,
      s1CompletedCount: 0,
      abCompletedCount: 2,
      checkInPoints: 10,
      trackedMinutes: 135,
      completedNodes: [1, 2],
      nextAuditEpochMillis: Date.now() + 3600000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(logRef.get());

  await assertSucceeds(
    logRef.update({
      dayState: "SLEEP",
      totalScore: 54.0,
      s1CompletedCount: 1,
      abCompletedCount: 3,
      checkInPoints: 20,
      trackedMinutes: 297,
      completedNodes: [1, 2, 3, 4],
      updatedAt: pastTime(1000),
    })
  );

  // Reject invalid dayState
  await assertFails(
    logRef.update({
      dayState: "NAP",
      updatedAt: pastTime(500),
    })
  );

  // Reject oversized completedNodes (> 7)
  await assertFails(
    logRef.update({
      completedNodes: [1, 2, 3, 4, 5, 6, 7, 8],
      updatedAt: pastTime(500),
    })
  );

  // Cross-user denial
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc(DATE_KEY).get());
});

test("TimeBlocks & CheckIns: valid immutable creations succeed; updates and out-of-range ratings fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTime(2000);

  const blockRef = aliceDb.collection("users").doc(ALICE_UID).collection("timeBlocks").doc("blk_1");
  await assertSucceeds(
    blockRef.set({
      userId: ALICE_UID,
      dateKey: DATE_KEY,
      taskId: "task_s1",
      taskBadge: "S-1",
      taskTitle: "Finalize core API architecture spec",
      tier: "S",
      startTimeLabel: "14:30",
      durationMinutes: 45,
      qualityRate: 8,
      finishingRate: 4,
      actionRate: 16,
      multiplier: 1.5,
      earnedPoints: 42.0,
      createdAt: now,
    })
  );

  // Immutable: updates must be denied
  await assertFails(blockRef.update({ earnedPoints: 99.0 }));

  // Out-of-bounds qualityRate (> 10) must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("timeBlocks").doc("blk_bad").set({
      userId: ALICE_UID,
      dateKey: DATE_KEY,
      taskId: "task_s1",
      taskBadge: "S-1",
      taskTitle: "Invalid rating",
      tier: "S",
      startTimeLabel: "14:30",
      durationMinutes: 45,
      qualityRate: 15,
      finishingRate: 4,
      actionRate: 16,
      multiplier: 1.5,
      earnedPoints: 42.0,
      createdAt: now,
    })
  );

  const checkInRef = aliceDb.collection("users").doc(ALICE_UID).collection("checkIns").doc("ci_3");
  await assertSucceeds(
    checkInRef.set({
      userId: ALICE_UID,
      dateKey: DATE_KEY,
      nodeIndex: 3,
      creativeIq: "Reframed schema decomposition cleanly.",
      emotionEq: "Calm, measured focus.",
      bestThinking: "Prioritized interface stability over premature optimization.",
      dailySevenPoint: "Removed notification noise before deciding.",
      durationSeconds: 84,
      pointsAwarded: 5,
      createdAt: now,
    })
  );

  await assertFails(checkInRef.update({ pointsAwarded: 50 }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("checkIns").doc("ci_3").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("checkIns").where("dateKey", "==", DATE_KEY).get()
  );
});
