<template>
  <a
  class="btn"
    @click="$emit('show-conversations')"
    title="Star a conversation about this note"
  >
    <SvgChat />
  </a>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from "vue"
import SvgChat from "../../svgs/SvgChat.vue"

defineProps<{
  noteId: number
}>()

defineEmits<{
  "show-conversations": []
}>()
</script>
