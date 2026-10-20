<template>
  <nav class="daisy-navbar max-w-full flex justify-between relative">
    <DonutMenu :user="user" />
    <div class="flex flex-1 min-w-0 overflow-x-auto border-l border-base-300 ml-1 pl-1">
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

const user = inject<Ref<User | undefined>>("currentUser")
</script>

<style scoped lang="scss">
@use "@/assets/page-bar.scss" as *;

.daisy-navbar {
  height: $page-bar-height;
  min-height: $page-bar-height;
  box-shadow: inset 0 -1px 0 var(--color-base-300);
}
</style>
