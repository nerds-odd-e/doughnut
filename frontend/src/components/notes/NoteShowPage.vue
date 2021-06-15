<template>
  <div v-if="note">
  <NoteControlHeader :note="note"/>
  <div class="jumbotron py-4 mb-2">
      <nav class="nav d-flex flex-row-reverse p-0">
          <NoteButtons :note="note"/>
      </nav>
      <NoteShow :note="note" :links="links" :level="level" :forBazaar="forBazaar"/>
  </div>
  <nav class="nav d-flex justify-content-between p-0 mb-2">
    <div class="btn-group btn-group-sm">
        <a :href="`/notes/${note.id}/move`">Move This Note</a>
    </div>
    <NoteNavigationButtons :urlPrefix="''" :navigation="navigation"/>
  </nav>
  </div>

</template>

<script setup>
import NoteShow from "./NoteShow.vue"
import NoteButtons from "./NoteButtons.vue"
import NoteNavigationButtons from "./NoteNavigationButtons.vue"
import NoteControlHeader from "./NoteControlHeader.vue"
import { ref, defineProps } from "vue"

const props = defineProps({noteid: Number, level: Number, forBazaar: Boolean})
const note = ref(null)
const links = ref(null)
const navigation = ref(null)
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
          navigation.value = articles.navigation;
        })
        .catch(error => {
          window.alert(error);
        });
    }

fetchData()
</script>
