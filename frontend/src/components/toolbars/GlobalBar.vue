<template>
  <nav class="navbar justify-content-between global-bar sticky-top bg-white">
    <div class="container-fluid d-flex">
      <PopButton v-if="user" title="open sidebar" :sidebar="'left'">
        <template #button_face>
          <SvgSidebar />
        </template>
        <GlobalSidebar
          :user="user"
          @update-user="$emit('updateUser', $event)"
        />
      </PopButton>
      <LoginButton v-else />

      <div class="d-flex flex-grow-1 justify-content-between">
        <div class="d-flex flex-grow-1" id="head-status" />
        <div class="btn-group btn-group-sm">
          <router-link to="/d/feedback">
            <div v-if="user" id="top-navbar-message-icon">
              <div v-if="unreadMessageCount > 0" class="unread-count">
                {{ unreadMessageCount }}
              </div>
              <SvgMessage />
            </div>
          </router-link>
          <PopButton v-if="user" title="search note">
            <template #button_face>
              <SvgSearch />
            </template>
            <template #default="{ closer }">
              <LinkNoteDialog
                v-bind="{ storageAccessor }"
                @close-dialog="closer"
              />
            </template>
          </PopButton>
          <NoteUndoButton v-bind="{ storageAccessor }" />
        </div>
        <ApiStatus
          v-if="user"
          :api-status="apiStatus"
          @clear-error-message="$emit('clearErrorMessage')"
        />
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { type ApiStatus } from "@/managedApi/ManagedApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { ref, watch } from "vue"
const props = defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  apiStatus: { type: Object as PropType<ApiStatus>, required: true },
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser", "clearErrorMessage"])
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
    fetchUnreadMessageCount()
  }
)
</script>

<style scoped lang="scss">
.global-bar {
  border-bottom: 1px solid #e9ecef;
}
</style>
