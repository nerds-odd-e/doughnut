<template>
  <NoteShowWithTitle v-bind="{note, links, level, owns}">
    <h2 :class="'h' + level"> {{note.noteContent.title}}</h2>
    <pre class="note-body" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
  </NoteShowWithTitle>

</template>

<script>
export default { name: "NoteShow" };
</script>

<script setup>
import NoteShowWithTitle from "./NoteShowWithTitle.vue"
import { ref, defineProps } from "vue"

const props = defineProps({note: {type: Object, required: true }, links: Object, level: Number, owns: Boolean})
</script>
