<template>
  <PopButton :title="`Star a conversation about this note`">
    <template #button_face>
      <SvgChat />
    </template>
    <template #default="{ closer }">
      <NoteSendMessageDialog
        :noteId="noteId"
        @submitted="closer()"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import { defineProps } from "vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteSendMessageDialog from "../NoteSendMessageDialog.vue"
import SvgChat from "../../svgs/SvgChat.vue"

defineProps<{
  noteId: number
}>()
</script>
