<template>
  <LoadingThinBar v-if="loading"/>
  <div v-if="note">
    <NoteControlHeader :note="note" :ancestors="ancestors" :ownership="ownership"/>
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
    <NoteOwnerViewCards :notes="childrenNotes"/>
  </div>
  <div v-else><ContentLoader /></div>

</template>

<script setup>
import NoteShow from "./NoteShow.vue"
import NoteButtons from "./NoteButtons.vue"
import NoteNavigationButtons from "./NoteNavigationButtons.vue"
import NoteControlHeader from "./NoteControlHeader.vue"
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue"
import ContentLoader from "../ContentLoader.vue"
import LoadingThinBar from "../LoadingThinBar.vue"
import { ref, watch, defineProps } from "vue"

const props = defineProps({noteid: Number, level: Number, forBazaar: Boolean})
const note = ref(null)
const links = ref(null)
const navigation = ref(null)
const childrenNotes = ref(null)
const ownership = ref(null)
const ancestors = ref(null)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  fetch(`/api/notes/${props.noteid}`)
    .then(res => {
      return res.json();
    })
    .then(noteViewedByUser => {
      note.value = noteViewedByUser.note;
      links.value = noteViewedByUser.links;
      navigation.value = noteViewedByUser.navigation;
      childrenNotes.value = noteViewedByUser.children;
      ownership.value = noteViewedByUser.ownership;
      ancestors.value = noteViewedByUser.ancestors;
      loading.value = false
    })
    .catch(error => {
      window.alert(error);
    });
}

watch(()=>props.noteid, ()=>fetchData())
fetchData()
</script>
