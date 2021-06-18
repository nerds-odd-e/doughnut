<template>
  <NoteShowWithTitle :note="note" :links="links" :level="level" :forBazaar="forBazaar">
    <h2 :class="'h' + level"> {{note.noteContent.title}}</h2>
    <pre class="note-body" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
  </NoteShowWithTitle>

</template>

<script setup>
import NoteShowWithTitle from "./NoteShowWithTitle.vue"
import { ref, defineProps } from "vue"

const props = defineProps({note: {tyep: Object, required: true }, links: Object, level: Number, forBazaar: Boolean})
</script>
