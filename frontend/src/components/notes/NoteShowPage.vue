<template>
  <div class="jumbotron py-4 mb-2">
  <div v-if="note">


      <nav class="nav d-flex flex-row-reverse p-0">
          <NoteButtons :note="note"/>
      </nav>
      <NoteShow :note="note" :links="links" :level="level" :forBazaar="forBazaar"/>
  </div>
  </div>

</template>

<script setup>
import NoteShow from "./NoteShow.vue"
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
