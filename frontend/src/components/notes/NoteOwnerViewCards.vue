<template>
<NoteCardWithHeaderAndButton :notes="notes">
     <template v-slot:headerButton="slotProps">
      <NoteButtons :note="slotProps.note"/>
     </template>
</NoteCardWithHeaderAndButton>
</template>

<script setup>
import NoteCardWithHeaderAndButton from "./NoteCardWithHeaderAndButton.vue"
import NoteButtons from "./NoteButtons.vue"
import { ref, defineProps } from "vue"

const props = defineProps({notes: Array})
</script>
