<script setup>
import NoteStatisticsButton from './components/notes/NoteStatisticsButton.vue'
import Repetition from './components/review/Repetition.vue'
import ContentLoader from "./components/ContentLoader.vue"
import LoadingThinBar from "./components/LoadingThinBar.vue"
import { ref, inject, watch } from 'vue'

const noteid = inject('noteid')
const linkid = inject('linkid')
const sadOnly = inject('sadOnly')
const reviewPointId = inject('reviewPointId')
const loading = ref(false)
const reviewPoint = ref(null)

const fetchData = async () => {
  loading.value = true
  fetch(`/api/review-points/${reviewPointId}`)
    .then(res => {
      return res.json();
    })
    .then(resp => {
      reviewPoint.value = resp;
      loading.value = false
    })
    .catch(error => {
      window.alert(error);
    });
}

fetchData()

</script>

<template>
  <LoadingThinBar v-if="loading"/>
  <div v-if="!!reviewPoint">
    <Repetition :reviewPoint="reviewPoint" :sadOnly="sadOnly"/>
  </div>
  <div v-else><ContentLoader /></div>
  <nav class="nav d-flex flex-row-reverse p-0">
    <NoteStatisticsButton :noteid="noteid" :linkid="linkid"/>
  </nav>
</template>