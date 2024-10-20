<template>
  <router-link to="/d/message-center">
    <div id="top-navbar-message-icon">
      <div v-if="messageCenterConversations.unreadConversations.length !== 0" class="unread-count">
        {{ messageCenterConversations.unreadConversations.length }}
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
  messageCenterConversations.unreadConversations =
    await managedApi.restConversationMessageController.getUnreadConversations()
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
