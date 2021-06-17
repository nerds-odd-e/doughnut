<template>
  <LoadingThinBar v-if="loading"/>
  <div v-if="noteViewedByUser">
    <NoteViewedByUser v-bind="noteViewedByUser"/>
  </div>
  <div v-else><ContentLoader /></div>

</template>

<script setup>
import NoteViewedByUser from "./NoteViewedByUser.vue"
import ContentLoader from "../ContentLoader.vue"
import LoadingThinBar from "../LoadingThinBar.vue"
import { ref, watch, defineProps } from "vue"

const props = defineProps({noteid: Number, level: Number, forBazaar: Boolean})
const noteViewedByUser = ref(null)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  fetch(`/api/notes/${props.noteid}`)
    .then(res => {
      return res.json();
    })
    .then(resp => {
      noteViewedByUser.value = resp;
      loading.value = false
    })
    .catch(error => {
      window.alert(error);
    });
}

watch(()=>props.noteid, ()=>fetchData())
fetchData()
</script>
