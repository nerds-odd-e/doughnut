<template>
  <router-link to="/d/message-center">
    <div id="top-navbar-message-icon">
      <div v-if="messageCenterConversations.unreadMessageCount !== 0" class="unread-count">
        {{ messageCenterConversations.unreadMessageCount }}
      </div>
      <SvgMessage />
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { watch } from "vue"
import type { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import { messageCenterConversations } from "@/store/messageStore"

const props = defineProps({
  user: { type: Object as PropType<User> },
})

const { managedApi } = useLoadingApi()

const fetchUnreadMessageCount = async () => {
  const unreadMessageCountResponse = (
    await managedApi.restConversationMessageController.getUnreadConversations()
  ).length
  messageCenterConversations.unreadMessageCount = unreadMessageCountResponse
}

watch(
  () => props.user,
  () => {
    if (props.user) {
      fetchUnreadMessageCount()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
/* Add any specific styles for the message center button here */
</style>
