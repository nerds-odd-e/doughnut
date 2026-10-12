<template>
  <button
    ref="helpButton"
    type="button"
    class="daisy-btn daisy-btn-outline daisy-btn-primary my-4"
    @click="startConversation"
  >
    <MessageCircle class="w-5 h-5" />
    Get private help
  </button>
  <Modal v-if="conversation && user" @close_request="closeHelp">
    <template #header><h2>Private recall help</h2></template>
    <template #body>
      <ConversationComponent
        :conversation="conversation"
        :user="user"
        @close-dialog="closeHelp"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { inject, nextTick, ref, type Ref } from "vue"
import { MessageCircle } from "@lucide/vue"
import type { Conversation, User } from "@generated/donut-backend-api"
import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import Modal from "@/components/commons/Modal.vue"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps<{
  recallPromptId: number
}>()

const user = inject<Ref<User | undefined>>("currentUser")
const conversation = ref<Conversation>()
const helpButton = ref<HTMLButtonElement>()

const startConversation = async () => {
  const { data, error } = await apiCallWithLoading(() =>
    ConversationMessageController.startConversationAboutRecallPrompt({
      path: { recallPrompt: props.recallPromptId },
    })
  )
  if (!error) {
    conversation.value = data
  }
}

const closeHelp = async () => {
  conversation.value = undefined
  await nextTick()
  helpButton.value?.focus()
}
</script>
