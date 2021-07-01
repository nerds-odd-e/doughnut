<template>
  <LoadingPage v-bind="{loading, contentExists: !!reviewPointForView}">
    <div v-if="!!reviewPointForView">
      <Repetition v-bind="{...reviewPointForView, sadOnly}"/>
    </div>
  </LoadingPage>
  <nav class="nav d-flex flex-row-reverse p-0">
    <NoteStatisticsButton v-blin="{noteid, linkid}"/>
  </nav>
</template>

<script setup>
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import Repetition from '../components/review/Repetition.vue'
import LoadingPage from "./commons/LoadingPage.vue"
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
