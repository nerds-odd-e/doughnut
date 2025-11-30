<template>
  <nav class="daisy-navbar daisy-max-w-full daisy-flex daisy-justify-between">
    <div class="daisy-flex daisy-flex-1 daisy-overflow-x-auto">
      <slot />
    </div>
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
  </nav>
</template>

<script setup lang="ts">
import type { Ref } from "vue"
import type { User } from "@generated/backend"
import { inject } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")
</script>

<style scoped lang="scss">
</style>
