<template>
  <ContentLoader v-if="isLoading" />
  <template v-else-if="user">
    <ConversationInner
      v-if="conversation"
      :conversation="conversation"
      :conversations="conversations"
      :user="user"
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
import ConversationTemplate from "@/components/conversations/ConversationTemplate.vue"
import ConversationInner from "./ConversationInner.vue"
import { inject, ref, type Ref, onMounted } from "vue"
import type { Conversation, User } from "@generated/backend"
import ContentLoader from "../commons/ContentLoader.vue"
import { ConversationMessageController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const conversation = ref<Conversation | undefined>()
const user = inject<Ref<User | undefined>>("currentUser")
const isLoading = ref(true)
const conversations = ref<Conversation[]>([])

const props = defineProps<{
  noteId: number
  isMaximized?: boolean
}>()
const emit = defineEmits(["submitted", "close-dialog", "toggle-maximize"])

const initialAiReply = ref(false)

onMounted(async () => {
  try {
    const { data: fetchedConversations, error } =
      await ConversationMessageController.getConversationsAboutNote({
        path: { note: props.noteId },
      })
    if (!error && fetchedConversations) {
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
  const { data: newConversation, error } = await apiCallWithLoading(() =>
    ConversationMessageController.startConversationAboutNote({
      path: { note: props.noteId },
      body: message,
    })
  )
  if (!error) {
    conversation.value = newConversation!
    emit("submitted")
  }
}

async function startConversationWithMessageAndAI(message: string) {
  initialAiReply.value = true
  const { data: newConversation, error } = await apiCallWithLoading(() =>
    ConversationMessageController.startConversationAboutNote({
      path: { note: props.noteId },
      body: message,
    })
  )
  if (!error) {
    conversation.value = newConversation!
    emit("submitted")
  }
}

const handleNewConversation = () => {
  conversation.value = undefined
}
</script>
