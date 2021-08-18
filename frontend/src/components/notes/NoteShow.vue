<template>
  <NoteShowWithTitle v-bind="{note, links, level, owns, colors}">
    <h2 :class="'h' + level"> {{note.noteContent.title}}</h2>
    <pre class="note-body" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
  </NoteShowWithTitle>

</template>

<script>
import NoteShowWithTitle from "./NoteShowWithTitle.vue"

export default {
  name: "NoteShow",
  props: {
    note: {type: Object, required: true },
    links: Object,
    level: { type: Number, default: 2 },
    owns: {type: Boolean, required: true},
    colors: Object },
  components: { NoteShowWithTitle }

}
</script>
