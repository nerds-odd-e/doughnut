<template>
  <div class="conversation-container">
    <!-- Upper half -->
    <div class="subject-container" v-if="!isMaximized">
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
      <AnsweredQuestionComponent
        v-else-if="conversation.subject?.answeredQuestion"
        v-bind="{
          answeredQuestion: conversation.subject.answeredQuestion,
          storageAccessor,
        }"
      />
    </div>

    <!-- Lower half -->
    <div class="conversation-messages" :class="{ 'maximized': isMaximized }">
      <ConversationInner
        v-bind="{
          conversation,
          conversations,
          user,
          storageAccessor,
          isMaximized
        }"
        @conversation-fetched="emit('conversation-fetched', $event)"
        @conversation-changed="handleConversationChange"
        @close-dialog="handleCloseDialog"
        @toggle-maximize="isMaximized = !isMaximized"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User, Conversation } from "@/generated/backend"
import NoteShow from "@/components/notes/NoteShow.vue"
import AssessmentQuestion from "@/components/assessment/AssessmentQuestion.vue"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { useRouter } from "vue-router"
import { ref, onMounted } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ConversationInner from "@/components/conversations/ConversationInner.vue"

const props = defineProps<{
  conversation: Conversation
  user: User
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
  (e: "conversation-changed", conversationId: number): void
}>()

const router = useRouter()
const { managedApi } = useLoadingApi()
const conversations = ref<Conversation[]>([])
const isMaximized = ref(false)

onMounted(async () => {
  if (props.conversation.subject?.note?.id) {
    conversations.value =
      await managedApi.restConversationMessageController.getConversationsAboutNote(
        props.conversation.subject.note.id
      )
  }
})

const handleConversationChange = (conversationId: number) => {
  const newConversation = conversations.value.find(
    (c) => c.id === conversationId
  )
  if (newConversation) {
    emit("conversation-changed", conversationId)
  }
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

.subject-container.minimized {
  height: 50px;
  overflow: hidden;
}

.conversation-messages.maximized {
  height: calc(100% - 50px);
}

</style>
