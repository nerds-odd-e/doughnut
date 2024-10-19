<template>
  <router-link to="/d/message-center">
    <div id="top-navbar-message-icon">
      <div v-if="unreadMessageCount > 0" class="unread-count">
        {{ unreadMessageCount }}
      </div>
      <SvgMessage />
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"

const props = defineProps({
  user: { type: Object as PropType<User> },
})

const { managedApi } = useLoadingApi()
const unreadMessageCount = ref(0)

const fetchUnreadMessageCount = async () => {
  const unreadMessageCountResponse =
    await managedApi.restConversationMessageController.getUnreadConversationCountOfCurrentUser()
  unreadMessageCount.value = unreadMessageCountResponse
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
