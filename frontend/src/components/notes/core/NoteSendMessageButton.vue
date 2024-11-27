<template>
  <a
    class="btn"
    role="button"
    @click="$emit('show-conversations')"
    title="Star a conversation about this note"
  >
    <SvgChat />
  </a>
</template>

<script setup lang="ts">
import SvgChat from "../../svgs/SvgChat.vue"

defineProps<{
  noteId: number
}>()

defineEmits<{
  "show-conversations": []
}>()
</script>
