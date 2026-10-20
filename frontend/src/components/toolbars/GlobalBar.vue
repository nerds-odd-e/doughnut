<template>
  <nav class="daisy-navbar max-w-full flex justify-between relative">
    <DonutMenu :user="user" />
    <div class="flex flex-1 overflow-x-auto">
      <slot />
    </div>
    <div class="daisy-join daisy-join-horizontal flex-none">
      <slot name="right" />
      <PopButton
        v-if="user"
        aria-label="search note"
        title="Search note (Ctrl+F / Cmd+F)"
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
import type { User } from "@generated/donut-backend-api"
import { inject } from "vue"
import { Search } from "@lucide/vue"
import DonutMenu from "@/components/toolbars/DonutMenu.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"

const user = inject<Ref<User | undefined>>("currentUser")
</script>

<style scoped lang="scss">
@use "@/assets/page-bar.scss" as *;

.daisy-navbar {
  height: $page-bar-height;
  min-height: $page-bar-height;
}
</style>
