<script setup>
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import Repetition from '../components/review/Repetition.vue'
import ContentLoader from "../components/ContentLoader.vue"
import LoadingThinBar from "../components/LoadingThinBar.vue"
import {restGet} from "../restful/restful"
import { ref, inject, watch } from 'vue'

const noteid = inject('noteid')
const linkid = inject('linkid')
const sadOnly = inject('sadOnly')
const reviewPointId = inject('reviewPointId')
const loading = ref(false)
const reviewPointForView = ref(null)

const fetchData = async () => {
  restGet(`/api/review-points/${reviewPointId}`, loading, (res)=>reviewPointForView.value = res)
}

fetchData()

</script>

<template>
  <LoadingThinBar v-if="loading"/>
  <div v-if="!!reviewPointForView">
    <Repetition v-bind="{...reviewPointForView, sadOnly}"/>
  </div>
  <div v-else><ContentLoader /></div>
  <nav class="nav d-flex flex-row-reverse p-0">
    <NoteStatisticsButton v-blin="{noteid, linkid}"/>
  </nav>
</template>