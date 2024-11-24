<template>
  <router-link to="/d/message-center" class="message-center-link">
    <div id="top-navbar-message-icon" class="d-flex flex-column align-items-center gap-1">
      <div class="icon-container">
        <div v-if="messageCenterConversations.unreadConversations.length !== 0" class="unread-count">
          {{ messageCenterConversations.unreadConversations.length }}
        </div>
        <slot />
      </div>
      <slot name="label" />
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
.message-center-link {
  text-decoration: none;
  color: inherit;
}

.icon-container {
  position: relative;
}

.unread-count {
  position: absolute;
  top: -8px;
  right: -8px;
  background: red;
  color: white;
  border-radius: 50%;
  min-width: 18px;
  height: 18px;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
</style>
