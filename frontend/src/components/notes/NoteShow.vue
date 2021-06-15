<template>
  <div v-if="note">

      <nav class="nav d-flex flex-row-reverse p-0">
          <NoteButtons :note="note"/>
      </nav>
      <NoteShowWithTitle :note="note" :links="links" :level="level" :forBazaar="forBazaar">
        <h2 :class="'h' + level"> {{note.noteContent.title}}</h2>
        <pre class="note-body" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
      </NoteShowWithTitle>
  </div>

</template>

<script setup>
import NoteShowWithTitle from "./NoteShowWithTitle.vue"
import NoteButtons from "./NoteButtons.vue"
import { ref, defineProps } from "vue"

const props = defineProps({noteid: Number, level: Number, forBazaar: Boolean})
const note = ref(null)
const links = ref(null)
const url = () => {
  return `/api/notes/${props.noteid}`
}
const fetchData = async () => {
      fetch(url())
        .then(res => {
          return res.json();
        })
        .then(articles => {
          note.value = articles.note;
          links.value = articles.links;
        })
        .catch(error => {
          window.alert(error);
        });
    }

fetchData()
</script>
