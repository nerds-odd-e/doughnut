# Private-audience prototype and overrun observation

**Identity:** SEED-073#story-1

Observed on 2026-10-12 during pre-refinement slice 5 in `/Users/terryyin/git/doughnut/.worktrees/resolve-a-confusing-recall-answer-with-an-explan`, branch `codex/resolve-a-confusing-recall-answer-with-an-explan`, Story Branch mode. The complete attempt was uncommitted on published `98996efafdb7847e36602aa5e477fe70d8d33255`; it was neither accepted nor delivered. Publisher `dashboard-territory.local-doughnut`, agent Ai-chan; authorized target `origin/main`, original claim `5d6307f63d716bd99b275bd2df88f3cca1f5499b`, starting revision `05b3651f2d9fb7e6f16a3e34447be602db4cc3b3`. [PLAN.md](PLAN.md) owns current statuses, split and receiving obligations.

## Observation and boundary

The complete prototype assigns initiating personal access, authorizes creation through the MemoryTracker owner, identifies a personal recall-help audience, and corrects the common incoming-message predicate. Direct controller fixtures use a learner's answered attempt on an author-created circle notebook with another member. Actual controllers drive creation, reads/messages, send, export, AI streaming, marking read, inbox listing and UserController's unread menu. Only the external OpenAI stream is mocked; these are authorization, persistence and consuming-menu observations, not generated-model quality.

The exact inspected [controller source](PRIVATE-ACCESS-PROTOTYPE-SOURCE.md) has 26 expanded cases. They cover learner access/export/reply, author/member refusal across direct channels, inbox exclusion, incoming assistant messages, another learner's prompt refusal, and retained legacy shared/personal labels. The original streamed-reply case observes stored reply → UserController unread menu → mark-read. Its result covers the complete prototype with the unread correction present.

Literal command, confirmed by the execution coordinator:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Result: full suite passed, 2763 tests, 0 failures/errors, 2 skipped. Inspection of 565 `backend/build/test-results/test/TEST-*.xml` files reproduced those totals. `TEST-com.odde.donut.controllers.PrivateRecallConversationControllerTest.xml` recorded 26 tests, 0 failures/errors/skips, timestamp `2026-10-12T07:22:50.463Z`, hostname `Territory.local`, test-case duration 0.122s. The latter is that class's measured case time, not overall suite or active-work time. A copy is parked at `/tmp/seed073-private-overrun/PrivateRecallConversationControllerTest.xml`. No product test was run during refinement.

Active implementation/proof writing accumulated about 10–11 minutes, crossing the ten-minute hard limit. Eight access consumers and necessary unread correction disproved the single audience-plus-inbox sizing premise. Passing the complete suite does not waive that overrun or accept a partial implementation. The full prototype and previous completed proofs remain available for scoped reuse.

## Incoming-message defect and split

The published JPQL uses `cm.sender != :user`; null-sender assistant messages are excluded by that comparison. The existing mark-read operation already treats a null sender as incoming. The prototype aligns the query with that existing meaning using `(cm.sender IS NULL OR cm.sender != :user)`, retaining the common conversation access predicate and existing read flag. This is an existing owner's local rule correction, not a new visibility predicate or notification resource.

Refined slice 5 restores only creation/personal-audience changes and proves confidentiality across every direct/discovery channel. Its unread exclusion proof must use an actual stored non-null learner message against author/member actors, for whom the sender differs; otherwise legacy null filtering would conceal an access leak. This legitimate learner message is a starting precondition, not an invented unauthorized sender. Existing shared reads/replies remain compatible. Positive assistant unread and explicit private audience labels move together to slice 6's inbox observation loop.

Until slice 6, messages can be read normally but the existing predicate excludes incoming AI replies, and partner labels use their existing behavior. G5/G6 receive these unfinished outcomes; there is no no-user-cost disposition. Later ordinary conversation, frontend entry and practice proof retain their separate owners. Each restored prefix must pass its own current full backend proof; this complete-prototype run establishes neither restored prefix independently.

## Safe parking and exact patch

Before refinement, the coordinator parked only six attempt-owned source paths, comprising the five tracked paths in this patch and the separate new controller source, then restored a clean working tree at the same published `98996efafdb7847e36602aa5e477fe70d8d33255`. Previous publications and unrelated work were preserved. Parked full copies and `attempt.patch` are in `/tmp/seed073-private-overrun`; the patch covers the five tracked paths, while the linked 211-line source preserves the sixth new path.

Patch SHA-256: `6c75c29a7282e2f75ef13edeb4247ada6ba750b7709b96427ae7f05f09332515`. The exact patch follows. Its retained code is historical prototype evidence, not current product state.

```diff
diff --git a/backend/src/main/java/com/odde/donut/controllers/ConversationMessageController.java b/backend/src/main/java/com/odde/donut/controllers/ConversationMessageController.java
index 921ce353a1..8179c1e906 100644
--- a/backend/src/main/java/com/odde/donut/controllers/ConversationMessageController.java
+++ b/backend/src/main/java/com/odde/donut/controllers/ConversationMessageController.java
@@ -162,7 +162,9 @@ public class ConversationMessageController {
 
   @PostMapping("/recall-prompt/{recallPrompt}")
   public Conversation startConversationAboutRecallPrompt(
-      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt) {
+      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt)
+      throws UnexpectedNoAccessRightException {
+    authorizationService.assertReadAuthorization(recallPrompt.requireMemoryTracker());
     return conversationService.startConversationAboutRecallPrompt(
         recallPrompt, authorizationService.getCurrentUser());
   }
diff --git a/backend/src/main/java/com/odde/donut/entities/Conversation.java b/backend/src/main/java/com/odde/donut/entities/Conversation.java
index 99da7fb26f..8791181efe 100644
--- a/backend/src/main/java/com/odde/donut/entities/Conversation.java
+++ b/backend/src/main/java/com/odde/donut/entities/Conversation.java
@@ -21,6 +21,7 @@ public class Conversation extends EntityIdentifiedByIdOnly {
   @Embedded private ConversationSubject subject = new ConversationSubject();
 
   @ManyToOne
+  @Setter
   @JoinColumn(name = "subject_ownership_id", referencedColumnName = "id")
   Ownership accessOwnership;
 
diff --git a/backend/src/main/java/com/odde/donut/entities/repositories/ConversationRepository.java b/backend/src/main/java/com/odde/donut/entities/repositories/ConversationRepository.java
index 77fc741a34..4abb5bcb39 100644
--- a/backend/src/main/java/com/odde/donut/entities/repositories/ConversationRepository.java
+++ b/backend/src/main/java/com/odde/donut/entities/repositories/ConversationRepository.java
@@ -37,7 +37,7 @@ public interface ConversationRepository extends CrudRepository<Conversation, Int
           + "WHERE ("
           + USER_CONVERSATION_CONDITION
           + ") "
-          + "AND cm.sender != :user "
+          + "AND (cm.sender IS NULL OR cm.sender != :user) "
           + "AND cm.readByReceiver IS NOT TRUE")
   List<ConversationMessage> findUnreadMessagesByUser(@Param("user") User user);
 }
diff --git a/backend/src/main/java/com/odde/donut/services/ConversationService.java b/backend/src/main/java/com/odde/donut/services/ConversationService.java
index 7871b3ad7c..9cb72b7e26 100644
--- a/backend/src/main/java/com/odde/donut/services/ConversationService.java
+++ b/backend/src/main/java/com/odde/donut/services/ConversationService.java
@@ -36,6 +36,7 @@ public class ConversationService {
       RecallPrompt recallPrompt, User initiator) {
     Conversation conversation = initializeConversation(initiator);
     conversation.setRecallPrompt(recallPrompt);
+    conversation.setAccessOwnership(initiator.getOwnership());
     conversationRepository.save(conversation);
     return conversation;
   }
@@ -74,6 +75,11 @@ public class ConversationService {
       return initiator.getName();
     }
     Ownership ownership = conversation.getAccessOwnership();
+    if (conversation.getSubject().getRecallPrompt() != null
+        && ownership != null
+        && Objects.equals(ownership.getUser(), initiator)) {
+      return "Private recall help";
+    }
     if (ownership != null && ownership.getCircle() != null) {
       return ownership.getCircle().getName();
     }
diff --git a/backend/src/test/java/com/odde/donut/controllers/ConversationStartControllerTest.java b/backend/src/test/java/com/odde/donut/controllers/ConversationStartControllerTest.java
index 5ac5a6fba0..f7f498614f 100644
--- a/backend/src/test/java/com/odde/donut/controllers/ConversationStartControllerTest.java
+++ b/backend/src/test/java/com/odde/donut/controllers/ConversationStartControllerTest.java
@@ -8,6 +8,7 @@ import com.odde.donut.entities.Conversation;
 import com.odde.donut.entities.MemoryTracker;
 import com.odde.donut.entities.Note;
 import com.odde.donut.entities.RecallPrompt;
+import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Nested;
 import org.junit.jupiter.api.Test;
@@ -55,20 +56,20 @@ class ConversationStartControllerTest extends ConversationMessageControllerTestB
     }
 
     @Test
-    void startsConversationAsCurrentUser() {
+    void startsConversationAsCurrentUser() throws UnexpectedNoAccessRightException {
       Conversation conversation = controller.startConversationAboutRecallPrompt(recallPrompt);
 
       assertThat(conversation.getConversationInitiator(), equalTo(currentUser.getUser()));
     }
 
     @Test
-    void setsRecallPromptAsSubject() {
+    void setsRecallPromptAsSubject() throws UnexpectedNoAccessRightException {
       Conversation conversation = controller.startConversationAboutRecallPrompt(recallPrompt);
       makeMe.refresh(conversation);
 
       assertThat(conversation.getSubject().getRecallPrompt(), equalTo(recallPrompt));
       assertThat(
-          conversation.getAccessOwnership(), equalTo(recallPrompt.getNotebook().getOwnership()));
+          conversation.getAccessOwnership(), equalTo(currentUser.getUser().getOwnership()));
     }
   }
 }
```
