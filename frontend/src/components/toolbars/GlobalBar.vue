<template>
  <nav class="daisy-navbar max-w-full flex justify-between relative">
    <div class="flex items-center shrink-0" :class="{ 'docked-contents': dockedContents }">
      <DonutMenu :user="user" />
      <slot name="navigation" />
    </div>
    <div class="flex flex-1 min-w-0 overflow-x-auto overflow-y-hidden border-l border-base-300 pl-1" :class="{ 'ml-1': !dockedContents }">
      <slot />
    </div>
    <div class="daisy-join daisy-join-horizontal flex-none">
      <slot name="right" />
      <NoteUndoButton />
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Ref } from "vue"
import type { User } from "@generated/donut-backend-api"
import { inject } from "vue"
import DonutMenu from "@/components/toolbars/DonutMenu.vue"

defineProps<{ dockedContents?: boolean }>()

const user = inject<Ref<User | undefined>>("currentUser")
</script>

<style scoped lang="scss">
@use "@/assets/page-bar.scss" as *;

.docked-contents {
  width: calc(var(--notebook-sidebar-width) - 0.5rem);
}

.daisy-navbar {
  height: $page-bar-height;
  min-height: $page-bar-height;
  box-shadow: inset 0 -1px 0 var(--color-base-300);
}
</style>
