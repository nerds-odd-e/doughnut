<template>
  <div class="conversation-container daisy-flex daisy-flex-col daisy-flex-1 min-h-0">
    <!-- Upper half -->
    <div v-if="!isMaximized" class="subject-container daisy-flex-1 daisy-overflow-auto daisy-p-4 daisy-border-b daisy-border-base-300">
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
          conversationButton: false,
          storageAccessor,
        }"
      />
    </div>

    <!-- Lower half -->
    <div class="daisy-flex-1 daisy-flex daisy-flex-col daisy-bg-base-200 min-h-0" :class="{ 'maximized': isMaximized }">
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
import type { User, Conversation } from "generated/backend"
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
.maximized {
  height: 100%;
}
</style>
