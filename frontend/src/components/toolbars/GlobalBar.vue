<template>
  <nav class="daisy-navbar daisy-max-w-full daisy-flex daisy-justify-between daisy-relative">
    <div class="daisy-flex daisy-flex-1 daisy-overflow-x-auto">
      <slot />
    </div>
    <div class="daisy-join daisy-join-horizontal daisy-flex-none">
      <slot name="right" />
      <PopButton
        v-if="user"
        ref="searchNotePopButtonRef"
        title="Search note (Ctrl+F / Cmd+F)"
        align-modal-top
        :show-close-button="false"
      >
        <template #button_face>
          <Search class="daisy-w-6 daisy-h-6" />
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
import { inject, onMounted, onUnmounted, ref } from "vue"
import { Search } from "lucide-vue-next"
import PopButton from "@/components/commons/Popups/PopButton.vue"

const user = inject<Ref<User | undefined>>("currentUser")

const searchNotePopButtonRef = ref<InstanceType<typeof PopButton> | null>(null)

function isBrowserFindShortcut(e: KeyboardEvent): boolean {
  if (!e.ctrlKey && !e.metaKey) return false
  if (e.shiftKey || e.altKey) return false
  return e.code === "KeyF" || e.key === "f" || e.key === "F"
}

function onWindowKeydownCapture(e: KeyboardEvent) {
  if (!user?.value || !isBrowserFindShortcut(e)) return
  e.preventDefault()
  searchNotePopButtonRef.value?.openDialog()
}

onMounted(() => {
  window.addEventListener("keydown", onWindowKeydownCapture, true)
})

onUnmounted(() => {
  window.removeEventListener("keydown", onWindowKeydownCapture, true)
})
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

@media (max-width: theme('screens.lg')) {
  .daisy-navbar {
    padding-left: $collapsed-menu-width-tablet;
    height: $main-menu-height-tablet;
    min-height: $main-menu-height-tablet;
  }
}

@media (max-width: theme('screens.md')) {
  .daisy-navbar {
    padding-left: $collapsed-menu-width-mobile;
    height: $main-menu-height-mobile;
    min-height: $main-menu-height-mobile;
  }
}
</style>
