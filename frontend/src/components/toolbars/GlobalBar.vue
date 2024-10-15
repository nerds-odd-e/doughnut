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
          <button v-if="user" class="btn btn-link" title="Message center link" @click="navigateToMessages">
            <SvgMessage />
          </button>
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
import { type ApiStatus } from "@/managedApi/ManagedApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { useRouter } from "vue-router"

defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  apiStatus: { type: Object as PropType<ApiStatus>, required: true },
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser", "clearErrorMessage"])

const router = useRouter()

const navigateToMessages = () => {
  router.push("/d/feedback")
}
</script>

<style scoped lang="scss">
.global-bar {
  border-bottom: 1px solid #e9ecef;
}
</style>
