<template>
  <ContentLoader v-if="isLoading" />
  <template v-else-if="user">
    <ConversationInner
      v-if="conversation"
      :conversation="conversation"
      :conversations="conversations"
      :user="user"
      :storage-accessor="storageAccessor"
      :allow-new-conversation="true"
      :initial-ai-reply="initialAiReply"
      :is-maximized="isMaximized"
      @close-dialog="$emit('close-dialog')"
      @conversation-changed="handleConversationChange"
      @new-conversation="handleNewConversation"
      @toggle-maximize="$emit('toggle-maximize')"
    />
    <ConversationTemplate
      v-else
      @send-message="startConversationWithMessage"
      @send-message-and-invite-ai="startConversationWithMessageAndAI"
      :conversations="conversations"
      @conversation-changed="handleConversationChange"
      @close-dialog="$emit('close-dialog')"
      @toggle-maximize="$emit('toggle-maximize')"
      :is-maximized="isMaximized"
    >
      <template #messages>
        <h2>Start a conversation about this note</h2>
        <p>
          <i>
            To talk with AI and/or other people who have access to this note.
          </i>
        </p>
      </template>
    </ConversationTemplate>
  </template>
</template>

<script setup lang="ts">
import useLoadingApi from "@/managedApi/useLoadingApi.ts"
import ConversationTemplate from "@/components/conversations/ConversationTemplate.vue"
import ConversationInner from "./ConversationInner.vue"
import { inject, ref, type Ref, onMounted } from "vue"
import type { Conversation, User } from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import ContentLoader from "../commons/ContentLoader.vue"

const conversation = ref<Conversation | undefined>()
const user = inject<Ref<User | undefined>>("currentUser")
const isLoading = ref(true)
const conversations = ref<Conversation[]>([])

const { managedApi } = useLoadingApi()
const props = defineProps<{
  noteId: number
  storageAccessor: StorageAccessor
  isMaximized?: boolean
}>()
const emit = defineEmits(["submitted", "close-dialog", "toggle-maximize"])

const initialAiReply = ref(false)

onMounted(async () => {
  try {
    const fetchedConversations =
      await managedApi.services.getConversationsAboutNote({
        path: { note: props.noteId },
      })
    if (fetchedConversations) {
      conversations.value = fetchedConversations
      conversation.value =
        fetchedConversations.length > 0 ? fetchedConversations[0] : undefined
    }
  } finally {
    isLoading.value = false
  }
})

const handleConversationChange = (conversationId: number) => {
  conversation.value = conversations.value.find((c) => c.id === conversationId)
}

async function startConversationWithMessage(message: string) {
  initialAiReply.value = false
  conversation.value = await managedApi.services.startConversationAboutNote({
    path: { note: props.noteId },
    body: message,
  })
  emit("submitted")
}

async function startConversationWithMessageAndAI(message: string) {
  initialAiReply.value = true
  conversation.value = await managedApi.services.startConversationAboutNote({
    path: { note: props.noteId },
    body: message,
  })
  emit("submitted")
}

const handleNewConversation = () => {
  conversation.value = undefined
}
</script>
