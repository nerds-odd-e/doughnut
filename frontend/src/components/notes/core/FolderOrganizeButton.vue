<template>
  <PopButton
    title="Folder…"
    aria-label="Folder…"
    align-modal-top
    btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
  >
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <FolderOrganizeForm
        :moving-folder-realm="movingFolderRealm"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import type { FolderRealm } from "@generated/doughnut-backend-api"
import PopButton from "../../commons/Popups/PopButton.vue"
import FolderOrganizeForm from "../FolderOrganizeForm.vue"

defineProps<{
  movingFolderRealm: FolderRealm
}>()
</script>
