<template>
  <ContentLoader v-if="isLoading" />
  <template v-else-if="user">
    <ConversationInner
      v-if="conversation"
      :conversation="conversation"
      :user="user"
      :storage-accessor="storageAccessor"
      @close-dialog="$emit('close-dialog')"
    />
    <ConversationTemplate
      v-else
      @send-message="startConversationWithMessage"
      @close-dialog="$emit('close-dialog')"
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
import type { Conversation, User } from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import ContentLoader from "../commons/ContentLoader.vue"

const conversation = ref<Conversation | undefined>()
const user = inject<Ref<User | undefined>>("currentUser")
const isLoading = ref(true)

const { managedApi } = useLoadingApi()
const props = defineProps<{
  noteId: number
  storageAccessor: StorageAccessor
}>()
const emit = defineEmits(["submitted", "close-dialog"])

onMounted(async () => {
  try {
    const conversations =
      await managedApi.restConversationMessageController.getConversationsAboutNote(
        props.noteId
      )
    conversation.value = conversations.length > 0 ? conversations[0] : undefined
  } finally {
    isLoading.value = false
  }
})

async function startConversationWithMessage(message: string) {
  conversation.value =
    await managedApi.restConversationMessageController.startConversationAboutNote(
      props.noteId,
      message
    )

  emit("submitted")
}
</script>
