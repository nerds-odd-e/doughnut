<template>
  <PopButton :title="`Send message about note top bazaar`">
    <template #button_face>
      <SvgMessage />
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
import SvgMessage from "../../svgs/SvgMessage.vue" // Make sure to import SvgMessage

defineProps<{
  noteId: number
}>()
</script>
