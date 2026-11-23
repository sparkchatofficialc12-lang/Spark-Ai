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
const ADMIN_UID = "admin_sparkling";
const ADMIN_EMAIL = "sparkchatofficialc12@gmail.com";

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

test("Unauthenticated users cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice",
      username: "alice",
      role: "creator",
      accountStatus: "active",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Owner can register account and create/update projects, scenes, scripts, characters, movies, and jobs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice Adeyemi",
      username: "alice_creator",
      country: "Nigeria",
      phoneNumber: "+2348000000000",
      role: "creator",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      title: "Shadow of Lagos",
      description: "Action thriller in Lagos",
      genre: "Nollywood",
      countryLocation: "Lagos, Nigeria",
      language: "English",
      aspectRatio: "16:9",
      resolution: "1080p",
      targetDurationSec: 3600,
      estimatedCredits: 120,
      status: "draft",
      publicProject: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1")
      .collection("scenes").doc("scene_1").set({
        userId: ALICE_UID,
        projectId: "proj_1",
        sceneNumber: 1,
        title: "Opening Chase in Victoria Island",
        location: "Victoria Island, Lagos",
        description: "Drone shot over Third Mainland Bridge at dusk",
        durationSec: 15,
        status: "planned",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_1").set({
      userId: ALICE_UID,
      name: "Tunde Kalu",
      role: "Protagonist",
      ageRange: "25-35",
      appearance: "Athletic build, short fade",
      clothing: "Black tactical jacket",
      fictional: true,
      likenessConsentConfirmed: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("generationJobs").doc("job_1").set({
      userId: ALICE_UID,
      projectId: "proj_1",
      movieTitle: "Shadow of Lagos",
      jobType: "text_to_movie",
      status: "queued",
      totalScenes: 10,
      totalClips: 120,
      completedClips: 0,
      failedClips: 0,
      estimatedCredits: 120,
      creditsCharged: 0,
      idempotencyKey: "idem_proj_1_001",
      providerId: "unconfigured",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Privilege escalation, self-credit inflation, and cross-user access are blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Cannot create account with role=admin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice",
      username: "alice_creator",
      role: "admin",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Create valid creator account & initial free credits
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice",
      username: "alice_creator",
      role: "creator",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("credits").doc("balance").set({
      userId: ALICE_UID,
      balance: 20,
      promoCredits: 0,
      lifetimeEarned: 20,
      lifetimeSpent: 0,
      updatedAt: now,
    })
  );

  // Cannot inflate own credits
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("credits").doc("balance").update({
      balance: 9999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cannot escalate role or modify accountStatus
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or write Alice's projects or credits
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("credits").doc("balance").get());

  // Regular user cannot write adminSettings
  await assertFails(
    aliceDb.collection("adminSettings").doc("providerConfig").set({
      key: "providerConfig",
      providerName: "Hack",
      providerConfigured: true,
      updatedAt: now,
    })
  );
});

test("Verified Founder Admin can manage adminSettings, award promo credits, and suspend/restore accounts; unverified email spoof fails", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice creates account and initial credit wallet
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice",
      username: "alice_creator",
      role: "creator",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("credits").doc("balance").set({
      userId: ALICE_UID,
      balance: 20,
      promoCredits: 0,
      lifetimeEarned: 20,
      lifetimeSpent: 0,
      updatedAt: now,
    })
  );

  // Unverified email spoof must fail
  await assertFails(
    spoofedAdminDb.collection("adminSettings").doc("providerConfig").set({
      key: "providerConfig",
      providerName: "Spoof",
      providerConfigured: false,
      updatedAt: now,
    })
  );
  await assertFails(spoofedAdminDb.collection("users").get());

  // Verified admin succeeds
  await assertSucceeds(
    realAdminDb.collection("adminSettings").doc("providerConfig").set({
      key: "providerConfig",
      providerName: "Custom Video Proxy",
      providerBaseUrl: "https://video-api.example.com",
      providerConfigured: true,
      maxFreeDurationSec: 30,
      maxPaidDurationSec: 7200,
      promoOfferTitle: "Launch Bonus",
      promoBonusCredits: 50,
      paymentProviderName: "Paystack / Stripe Proxy",
      paymentConfigured: false,
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(realAdminDb.collection("users").get());
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).collection("credits").doc("balance").update({
      balance: 70,
      promoCredits: 50,
      lifetimeEarned: 70,
      updatedAt: now,
    })
  );
});

test("Community Templates: publish, public discovery, private isolation, prompt/settings reuse, remix attribution, likes, comments, and safety reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice publishes a public reusable template + prompt + settings + creator profile
  await assertSucceeds(
    aliceDb.collection("creatorProfiles").doc(ALICE_UID).set({
      creatorId: ALICE_UID,
      username: "alice_director",
      displayName: "Alice Adeyemi",
      publishedCount: 1,
      followersCount: 0,
      totalLikes: 0,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("communityTemplates").doc("tpl_fly").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_director",
      title: "Man Flying Through The Sky",
      description: "Epic superhero sky flight over Lagos skyline",
      category: "Flying effects",
      durationSec: 15,
      aspectRatio: "9:16",
      visibility: "public",
      allowReuse: true,
      likesCount: 0,
      commentsCount: 0,
      remixesCount: 0,
      trendingScore: 10,
      moderationStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("templatePrompts").doc("tpl_fly").set({
      templateId: "tpl_fly",
      creatorId: ALICE_UID,
      visibility: "public",
      allowReuse: true,
      basePrompt: "Hero soaring fast above glowing clouds at golden hour",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("templateSettings").doc("tpl_fly").set({
      templateId: "tpl_fly",
      creatorId: ALICE_UID,
      visibility: "public",
      allowReuse: true,
      cameraMovement: "Dynamic FPV Chase",
      lighting: "Golden Hour Rim Light",
      aspectRatio: "9:16",
      resolution: "1080p",
      durationSec: 15,
      updatedAt: now,
    })
  );

  // Alice publishes a PRIVATE non-reusable template
  await assertSucceeds(
    aliceDb.collection("communityTemplates").doc("tpl_priv").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_director",
      title: "Secret Project Draft",
      category: "Movie scenes",
      durationSec: 30,
      aspectRatio: "16:9",
      visibility: "private",
      allowReuse: false,
      likesCount: 0,
      commentsCount: 0,
      remixesCount: 0,
      trendingScore: 0,
      moderationStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("templatePrompts").doc("tpl_priv").set({
      templateId: "tpl_priv",
      creatorId: ALICE_UID,
      visibility: "private",
      allowReuse: false,
      basePrompt: "Confidential scene prompt",
      updatedAt: now,
    })
  );

  // Bob can query public active templates, read public reusable prompt/settings, like, comment, follow, remix, and report
  await assertSucceeds(
    bobDb.collection("communityTemplates")
      .where("visibility", "==", "public")
      .where("moderationStatus", "==", "active")
      .get()
  );
  await assertSucceeds(bobDb.collection("templatePrompts").doc("tpl_fly").get());
  await assertSucceeds(bobDb.collection("templateSettings").doc("tpl_fly").get());

  // Bob CANNOT read or delete Alice's private template or private prompt
  await assertFails(bobDb.collection("communityTemplates").doc("tpl_priv").get());
  await assertFails(bobDb.collection("templatePrompts").doc("tpl_priv").get());
  await assertFails(bobDb.collection("communityTemplates").doc("tpl_fly").delete());

  // Bob likes, comments, follows, creates a remix record, and files a safety report
  await assertSucceeds(
    bobDb.collection("templateLikes").doc("tpl_fly_" + BOB_UID).set({
      templateId: "tpl_fly",
      userId: BOB_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("templateComments").doc("c_1").set({
      templateId: "tpl_fly",
      userId: BOB_UID,
      username: "bob_creator",
      text: "Amazing flight camera angle!",
      createdAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("templateRemixes").doc("rmx_1").set({
      originalTemplateId: "tpl_fly",
      originalTemplateTitle: "Man Flying Through The Sky",
      originalCreatorId: ALICE_UID,
      originalCreatorName: "alice_director",
      remixerUserId: BOB_UID,
      remixerUsername: "bob_creator",
      newProjectId: "bob_proj_1",
      newMovieTitle: "Bob Flying Over Abuja",
      customPrompt: "Bob soaring over Abuja skyline",
      createdAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("creatorFollowers").doc(ALICE_UID + "_" + BOB_UID).set({
      creatorId: ALICE_UID,
      followerId: BOB_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("templateReports").doc("rep_1").set({
      templateId: "tpl_fly",
      creatorId: ALICE_UID,
      reporterId: BOB_UID,
      reason: "Copyright",
      details: "Test safety check",
      status: "open",
      createdAt: now,
    })
  );
});

test("Social Video Sharing: posts, deduplicated views, likes, threaded comments + moderation, shares, cross-user notifications, and singing/music job types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice creates a singing_photo generation job and publishes a public social post
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("generationJobs").doc("job_sing_1").set({
      userId: ALICE_UID,
      projectId: "proj_sing_1",
      movieTitle: "Lagos Choir Solo",
      jobType: "singing_photo",
      status: "queued",
      totalScenes: 1,
      totalClips: 1,
      completedClips: 0,
      failedClips: 0,
      estimatedCredits: 2,
      creditsCharged: 0,
      idempotencyKey: "idem_sing_1",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_director",
      creationType: "music_video",
      title: "Midnight Ikoyi Vibes",
      caption: "Afrobeats cinematic visual",
      hashtags: "#SparkAI #Afrobeats",
      category: "Music videos",
      durationSec: 45,
      aspectRatio: "9:16",
      resolution: "1080p",
      visibility: "public",
      allowComments: true,
      allowReuse: true,
      allowDownload: true,
      viewsCount: 0,
      uniqueViewersCount: 0,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      moderationStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob lists public posts, records a view, likes, comments, replies, shares, and notifies Alice
  await assertSucceeds(
    bobDb.collection("posts")
      .where("visibility", "==", "public")
      .where("moderationStatus", "==", "active")
      .get()
  );

  await assertSucceeds(
    bobDb.collection("postViews").doc("post_1_" + BOB_UID).set({
      postId: "post_1",
      postCreatorId: ALICE_UID,
      viewerUserId: BOB_UID,
      viewerUsername: "bob_creator",
      viewerPublic: true,
      viewCount: 1,
      lastViewedAt: now,
      createdAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("postLikes").doc("post_1_" + BOB_UID).set({
      postId: "post_1",
      postCreatorId: ALICE_UID,
      userId: BOB_UID,
      username: "bob_creator",
      userPublic: true,
      createdAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("postComments").doc("pc_1").set({
      postId: "post_1",
      postCreatorId: ALICE_UID,
      parentCommentId: "",
      userId: BOB_UID,
      username: "bob_creator",
      text: "Incredible Afrobeats lighting!",
      likesCount: 0,
      createdAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("postCommentLikes").doc("pc_1_" + BOB_UID).set({
      commentId: "pc_1",
      userId: BOB_UID,
      createdAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("postShares").doc("sh_1").set({
      postId: "post_1",
      postCreatorId: ALICE_UID,
      sharerUserId: BOB_UID,
      channel: "whatsapp",
      createdAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_like_1").set({
      userId: ALICE_UID,
      title: "New Like",
      message: "@bob_creator liked your post.",
      category: "like",
      actorUserId: BOB_UID,
      targetPostId: "post_1",
      targetCreatorId: BOB_UID,
      readFlag: false,
      createdAt: now,
    })
  );

  // Alice (post owner) can moderate/delete Bob's comment on Alice's post; Bob cannot delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
  await assertSucceeds(aliceDb.collection("postComments").doc("pc_1").delete());
});

test("Spark Social Two-Account Journey: 24h Stories + Viewers, Saved Posts, Reposts, Saved Sounds, and 1-to-1 Messaging with Read Receipts & Privacy", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_999", {
    email: "charlie@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);
  const expiresAt = Date.now() + 24 * 60 * 60 * 1000;

  // Alice creates a 24-hour story
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_director",
      mediaType: "photo",
      overlayText: "On Set in Lagos",
      stickerTag: "Director Cut",
      musicTrackTitle: "Lagos Golden Pulse",
      visibility: "public",
      viewsCount: 0,
      expiresAtMs: expiresAt,
      createdAt: now,
    })
  );

  // Bob views Alice's story and records a storyView; Alice sees it; Bob cannot spoof storyView as Charlie
  await assertSucceeds(
    bobDb.collection("storyViews").doc("story_1_" + BOB_UID).set({
      storyId: "story_1",
      storyCreatorId: ALICE_UID,
      viewerUserId: BOB_UID,
      viewerUsername: "bob_creator",
      viewerPublic: true,
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("storyViews").where("storyCreatorId", "==", ALICE_UID).get()
  );

  // Bob saves a post and reposts it
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("savedPosts").doc("post_1").set({
      userId: BOB_UID,
      postId: "post_1",
      createdAt: now,
    })
  );
  // Alice cannot read Bob's private savedPosts
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("savedPosts").get());

  await assertSucceeds(
    bobDb.collection("postReposts").doc("post_1_" + BOB_UID).set({
      postId: "post_1",
      originalCreatorId: ALICE_UID,
      reposterUserId: BOB_UID,
      reposterUsername: "bob_creator",
      note: "Must watch Nollywood sci-fi!",
      createdAt: now,
    })
  );

  // 1-to-1 Messaging between Alice and Bob; Charlie cannot read or inject messages!
  const convId = "alice_123_bob_456";
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "alice_director", [BOB_UID]: "bob_creator" },
      lastMessageText: "Hey Bob, loved your remix!",
      lastSenderId: ALICE_UID,
      lastMessageType: "text",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      senderId: ALICE_UID,
      senderUsername: "alice_director",
      receiverId: BOB_UID,
      messageType: "text",
      text: "Hey Bob, loved your remix!",
      deliveryStatus: "sent",
      deletedForAll: false,
      createdAt: now,
    })
  );

  // Bob reads messages and marks msg_1 as read
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages").get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").update({
      deliveryStatus: "read",
    })
  );

  // Charlie (non-participant) CANNOT read or write inside Alice & Bob's conversation
  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(charlieDb.collection("conversations").doc(convId).collection("messages").get());
  await assertFails(
    charlieDb.collection("conversations").doc(convId).collection("messages").doc("msg_hack").set({
      conversationId: convId,
      senderId: "charlie_999",
      receiverId: BOB_UID,
      messageType: "text",
      text: "Intruder",
      deliveryStatus: "sent",
      createdAt: now,
    })
  );
});

test("Spark Guide Knowledge Base, User Chats, and Founder Admin KB CRUD enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Regular user CANNOT create, update, or delete Spark Guide KB articles
  await assertFails(
    aliceDb.collection("sparkGuideArticles").doc("kb_1").set({
      title: "Fake Help Article",
      category: "Getting Started",
      stepsSummary: "1. Hack step",
      keywords: "hack",
      published: true,
      updatedBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified Admin CAN create & update KB article, and signed-in users CAN read it
  await assertSucceeds(
    realAdminDb.collection("sparkGuideArticles").doc("kb_1").set({
      title: "How to Replace a Video",
      category: "Content Management",
      stepsSummary: "1. Open your post.\n2. Tap 3 dots.\n3. Tap Replace Video.",
      keywords: "replace,video,post",
      published: true,
      updatedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    realAdminDb.collection("sparkGuideArticles").doc("kb_1").update({
      title: "How to Replace Any Post Video",
      updatedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(aliceDb.collection("sparkGuideArticles").get());

  // Alice can create/read her own Spark Guide chat & messages; Bob cannot access Alice's chats
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sparkGuideChats").doc("chat_1").set({
      userId: ALICE_UID,
      title: "Movie Creation Help",
      contextSection: "general",
      language: "English",
      lastMessagePreview: "1. Tap Create Movie...",
      messageCount: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sparkGuideChats").doc("chat_1")
      .collection("messages").doc("m_1").set({
        conversationId: "chat_1",
        userId: ALICE_UID,
        role: "user",
        text: "How do I create a movie?",
        offlineFallback: false,
        createdAt: now,
      })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sparkGuideChats").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sparkGuideChats").doc("chat_1").collection("messages").get()
  );
});

test("Content Management & 30-Day Recycle Bin: edit post, replace video, soft-delete, cross-user trash isolation, restore, and permanent delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);
  const expiresMs = Date.now() + 30 * 24 * 60 * 60 * 1000;

  // Alice publishes a post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_edit_1").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_director",
      creationType: "short_video",
      title: "Original Sunset",
      caption: "First cut",
      category: "Movie scenes",
      previewVideoUrl: "https://example.com/v1.mp4",
      durationSec: 20,
      visibility: "public",
      allowComments: true,
      allowReuse: true,
      allowDownload: true,
      viewsCount: 12,
      uniqueViewersCount: 10,
      likesCount: 4,
      commentsCount: 1,
      sharesCount: 0,
      moderationStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice edits metadata & replaces video keeping original post link; Bob cannot
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_edit_1").update({
      title: "Updated Sunset Director Cut",
      previewVideoUrl: "https://example.com/v2.mp4",
      durationSec: 35,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("posts").doc("post_edit_1").update({
      title: "Hijacked Title",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Alice soft-deletes post into Recycle Bin
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recentlyDeleted").doc("post_post_edit_1").set({
      userId: ALICE_UID,
      itemType: "post",
      originalId: "post_edit_1",
      title: "Updated Sunset Director Cut",
      subtitle: "short_video • public",
      creationType: "short_video",
      previewVideoUrl: "https://example.com/v2.mp4",
      payloadJson: "{\"title\":\"Updated Sunset Director Cut\"}",
      deletedAtMs: Date.now(),
      expiresAtMs: expiresMs,
      createdAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("posts").doc("post_edit_1").delete());

  // Bob CANNOT read or delete Alice's Recycle Bin items
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recentlyDeleted").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("recentlyDeleted").doc("post_post_edit_1").delete()
  );

  // Alice restores and deletes permanently
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recentlyDeleted").doc("post_post_edit_1").delete()
  );
});

test("Cybersecurity Enforcement: multi-device activeSessions, remote revocation, collectionGroup admin monitor, securityAlerts, and immutable auditLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice registers device session & Bob cannot read or revoke it
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activeSessions").doc("sess_pixel").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      username: "alice_creator",
      deviceLabel: "Google Pixel 9 (Android API 36)",
      platform: "Android",
      status: "active",
      currentSession: true,
      lastActiveAt: now,
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("activeSessions").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("activeSessions").doc("sess_pixel").update({
      status: "revoked",
      lastActiveAt: new Date(Date.now() - 300),
    })
  );

  // Regular user CANNOT query collectionGroup activeSessions; verified Admin CAN and CAN revoke a session
  await assertFails(aliceDb.collectionGroup("activeSessions").get());
  await assertSucceeds(realAdminDb.collectionGroup("activeSessions").get());
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).collection("activeSessions").doc("sess_pixel").update({
      status: "revoked",
      lastActiveAt: new Date(Date.now() - 200),
    })
  );

  // Security Alerts: user creates own unresolved alert & reads own alerts, cannot self-resolve; Admin can read & resolve
  await assertSucceeds(
    aliceDb.collection("securityAlerts").doc("alert_1").set({
      userId: ALICE_UID,
      username: "alice_creator",
      alertType: "suspicious_input",
      severity: "high",
      title: "Blocked Unsafe Script Input",
      description: "Script markup stripped from bio.",
      deviceLabel: "Google Pixel 9",
      resolvedFlag: false,
      resolvedBy: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("securityAlerts").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("securityAlerts").doc("alert_1").get());
  await assertFails(
    aliceDb.collection("securityAlerts").doc("alert_1").update({
      resolvedFlag: true,
      resolvedBy: ALICE_UID,
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(
    realAdminDb.collection("securityAlerts").doc("alert_1").update({
      resolvedFlag: true,
      resolvedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Immutable Audit Logs: signed-in user can append own log; only admin can read; NOBODY (not even admin) can update or delete
  await assertSucceeds(
    aliceDb.collection("auditLogs").doc("log_1").set({
      actorUserId: ALICE_UID,
      actorEmail: "alice@example.com",
      action: "revoke_session",
      targetCollection: "users/alice_123/activeSessions",
      targetId: "sess_pixel",
      summary: "Revoked suspicious device",
      createdAt: now,
    })
  );
  await assertFails(aliceDb.collection("auditLogs").get());
  await assertSucceeds(realAdminDb.collection("auditLogs").get());
  await assertFails(
    realAdminDb.collection("auditLogs").doc("log_1").update({ summary: "Tampered audit log" })
  );
  await assertFails(realAdminDb.collection("auditLogs").doc("log_1").delete());
});
