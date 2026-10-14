<template>
  <div class="flex" :class="{ 'justify-end': isCurrentUser }">
    <div
      v-if="!isCurrentUser"
      class="message-avatar me-2"
      :title="message.sender?.name || 'AI Assistant'"
    >
      <Bot v-if="message.sender?.id === undefined" class="w-8 h-8" />
      <UserIcon v-else class="w-8 h-8" />
    </div>

    <div
      class="daisy-card py-2 px-3"
      :class="[
        isCurrentUser ? 'bg-primary text-primary-content' : 'bg-base-200',
        message.sender?.id === undefined ? 'ai-assistant' : '',
      ]"
    >
      <pre v-if="isCurrentUser" class="user-message">{{ formattedMessage }}</pre>
      <div
        v-else
        :class="RICH_CONTENT_PROSE"
        v-html="markdownizer.markdownToHtml(formattedMessage)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { ConversationMessage, User } from "@generated/donut-backend-api"
import { Bot, User as UserIcon } from "@lucide/vue"
import markdownizer from "@/components/form/markdownizer"
import { RICH_CONTENT_PROSE } from "@/constants/richContentProse"

const props = defineProps<{
  message: ConversationMessage
  user: User
}>()
const isCurrentUser = computed(
  () => (props.message.sender?.id || 0) === props.user?.id
)
const formattedMessage = computed(() =>
  props.message.message.replace(/^"|"$/g, "").trim()
)
</script>

<style scoped lang="scss">
.message-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-message {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
}
</style>
