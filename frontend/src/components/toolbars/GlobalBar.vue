<template>
  <nav class="daisy-navbar daisy-max-w-full daisy-flex daisy-justify-between">
    <div class="daisy-flex daisy-flex-1 daisy-overflow-x-auto" id="head-status" />
    <div class="daisy-join daisy-join-horizontal daisy-flex-none">
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
    <LoadingThinBar v-if="user && apiStatus.states.length > 0" />
  </nav>
</template>

<script setup lang="ts">
import type { User } from "@generated/backend"
import { type ApiStatus } from "@/managedApi/ApiStatusHandler"
import LoadingThinBar from "@/components/commons/LoadingThinBar.vue"
import type { PropType } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

defineProps({
  apiStatus: { type: Object as PropType<ApiStatus>, required: true },
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser"])
</script>

<style scoped lang="scss">
.daisy-navbar {
  min-height: 100%;
}
</style>
