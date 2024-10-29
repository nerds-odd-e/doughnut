<template>
   <ConversationTemplate @send-message="submitMessage" >
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

<script setup lang="ts">
import useLoadingApi from "@/managedApi/useLoadingApi.ts"
import ConversationTemplate from "@/components/conversations/ConversationTemplate.vue"

const { managedApi } = useLoadingApi()
const props = defineProps<{
  noteId: number
}>()
const emit = defineEmits(["submitted"])

async function submitMessage(message: string) {
  await managedApi.restConversationMessageController.startConversationAboutNote(
    props.noteId,
    message
  )

  emit("submitted")
}
</script>
