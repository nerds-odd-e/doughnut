<template>
  <div class="conversation-container flex flex-col lg:flex-row flex-1 min-h-0">
    <div
      v-if="!isMaximized"
      class="subject-container flex-1 min-w-0 overflow-auto p-4 border-b border-base-300 lg:order-last lg:border-b-0 lg:border-l"
      :class="{ 'hidden lg:block': subjectNoteId }"
    >
      <NoteContextReader v-if="subjectNoteId" :note-id="subjectNoteId" />
      <AnsweredQuestionComponent
        v-else-if="subjectAnsweredQuestion"
        v-bind="{
          answeredQuestion: subjectAnsweredQuestion,
          conversationButton: false,
          reviewClarification: true,
        }"
      />
    </div>

    <div class="flex-1 flex flex-col bg-base-200 min-h-0 min-w-0" :class="{ 'maximized': isMaximized }">
      <ConversationInner
        v-bind="{
          conversation,
          conversations,
          user,
          isMaximized
        }"
        @conversation-changed="handleConversationChange"
        @close-dialog="handleCloseDialog"
        @toggle-maximize="isMaximized = !isMaximized"
        @attempt-excluded="handleAttemptExcluded"
      >
        <template #header-actions v-if="subjectNoteId">
          <PopButton
            class="lg:hidden"
            sidebar="right"
            aria-label="Read note context"
            title="Read note context"
          >
            <template #button_face><FileText class="w-6 h-6" /></template>
            <div class="min-w-0 max-w-[calc(100vw-50px)] px-4 pb-4 pt-10">
              <NoteContextReader :note-id="subjectNoteId" />
            </div>
          </PopButton>
        </template>
      </ConversationInner>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  AnsweredQuestion,
  User,
  Conversation,
} from "@generated/donut-backend-api"
import NoteContextReader from "@/components/notes/NoteContextReader.vue"
import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import { FileText } from "@lucide/vue"
import { useRouter } from "vue-router"
import { computed, ref, onMounted } from "vue"
import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import ConversationInner from "@/components/conversations/ConversationInner.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"

const props = defineProps<{
  conversation: Conversation
  user: User
}>()

const emit = defineEmits<{
  (e: "conversation-changed", conversationId: number): void
  (e: "close-dialog"): void
  (e: "attempt-excluded", answeredQuestion: AnsweredQuestion): void
}>()

const router = useRouter()
const conversations = ref<Conversation[]>([])
const isMaximized = ref(false)
const subjectNoteId = computed(() => props.conversation.subject?.note?.id)
const excludedAttempt = ref<AnsweredQuestion>()
const subjectAnsweredQuestion = computed(() => {
  const subject = props.conversation.subject?.recallPrompt
  return excludedAttempt.value?.id === subject?.id
    ? excludedAttempt.value
    : subject
})

const handleAttemptExcluded = (answeredQuestion: AnsweredQuestion) => {
  excludedAttempt.value = answeredQuestion
  emit("attempt-excluded", answeredQuestion)
}

onMounted(async () => {
  if (subjectNoteId.value) {
    const { data: conversationsList, error } =
      await ConversationMessageController.getConversationsAboutNote({
        path: { note: subjectNoteId.value },
      })
    if (!error) {
      conversations.value = conversationsList!
    }
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
  const noteTopology = props.conversation.subject?.note?.noteTopology
  if (noteTopology) {
    router.push(noteShowLocation(noteTopology.id))
  } else {
    emit("close-dialog")
  }
}
</script>

<style scoped>
.maximized {
  height: 100%;
}
</style>
