<template>
<NoteCardWithHeaderAndButton :notes="notes">
     <template v-if="owns" #headerButtons="{note}">
      <NoteButtons :note="note"/>
     </template>
</NoteCardWithHeaderAndButton>
</template>

<script setup>
import NoteCardWithHeaderAndButton from "./NoteCardWithHeaderAndButton.vue"
import NoteButtons from "./NoteButtons.vue"
import { ref, defineProps } from "vue"

const props = defineProps({notes: Array, owns: Boolean})
</script>
