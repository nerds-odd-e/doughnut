<template>
  <nav class="daisy-navbar max-w-full flex justify-between relative">
    <div class="flex flex-1 overflow-x-auto">
      <slot />
    </div>
    <div class="daisy-join daisy-join-horizontal flex-none">
      <slot name="right" />
      <PopButton
        v-if="user"
        ref="searchNotePopButtonRef"
        aria-label="search note"
        title="Search note (Ctrl+F / Cmd+F)"
        align-modal-top
        :show-close-button="false"
      >
        <template #button_face>
          <Search class="w-6 h-6" />
        </template>
        <template #default="{ closer }">
          <SearchForm :modal-closer="closer" @close-dialog="closer" />
        </template>
      </PopButton>
      <NoteUndoButton />
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Ref } from "vue"
import type { User } from "@generated/doughnut-backend-api"
import { inject, ref, watch } from "vue"
import { Search } from "@lucide/vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import {
  registerGlobalNoteSearchOpener,
  unregisterGlobalNoteSearchOpener,
} from "@/utils/globalNoteSearchShortcut"

const user = inject<Ref<User | undefined>>("currentUser")

const searchNotePopButtonRef = ref<InstanceType<typeof PopButton> | null>(null)

const openNoteSearch = () => {
  searchNotePopButtonRef.value?.openDialog()
}

watch(
  () => user?.value,
  (loggedIn, _, onCleanup) => {
    if (!loggedIn) return
    registerGlobalNoteSearchOpener(openNoteSearch)
    onCleanup(() => unregisterGlobalNoteSearchOpener(openNoteSearch))
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

@media (max-width: 1024px) {
  .daisy-navbar {
    padding-left: $collapsed-menu-width-tablet;
    height: $main-menu-height-tablet;
    min-height: $main-menu-height-tablet;
  }
}

@media (max-width: 768px) {
  .daisy-navbar {
    padding-left: $collapsed-menu-width-mobile;
    height: $main-menu-height-mobile;
    min-height: $main-menu-height-mobile;
  }
}
</style>
