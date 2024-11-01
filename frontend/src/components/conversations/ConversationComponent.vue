<template>
  <div class="conversation-container">
    <!-- Upper half -->
    <div class="subject-container">
      <NoteShow
        v-if="conversation.subject?.note?.id"
        v-bind="{
          noteId: conversation.subject?.note?.id,
          storageAccessor,
          expandChildren: false,
          noConversationButton: true,
        }"
      />
      <AssessmentQuestion
        v-else-if="conversation.subject?.assessmentQuestionInstance"
        v-bind="{
          assessmentQuestionInstance: conversation.subject?.assessmentQuestionInstance,
        }"
      />
    </div>

    <!-- Lower half -->
    <div class="conversation-messages">
      <ConversationInner
        v-bind="{ 
          conversation, 
          conversations,
          user, 
          storageAccessor 
        }"
        @conversation-fetched="emit('conversation-fetched', $event)"
        @conversation-changed="handleConversationChange"
        @close-dialog="handleCloseDialog"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User, Conversation } from "@/generated/backend"
import NoteShow from "@/components/notes/NoteShow.vue"
import AssessmentQuestion from "@/components/assessment/AssessmentQuestion.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { useRouter } from "vue-router"

const props = defineProps<{
  conversation: Conversation
  conversations?: Conversation[]
  user: User
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
  (e: "conversation-changed", conversationId: number): void
}>()

const router = useRouter()

const handleConversationChange = (conversationId: number) => {
  emit("conversation-changed", conversationId)
}

const handleCloseDialog = () => {
  if (props.conversation.subject?.note?.id) {
    router.push({
      name: "noteShow",
      params: { noteId: props.conversation.subject.note.id },
    })
  }
}
</script>

<style scoped>
.conversation-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.subject-container {
  flex: 1;
  overflow-y: auto;
  border-bottom: 1px solid #dee2e6;
  padding: 1rem;
}

.conversation-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  min-height: 0;
}

</style>
