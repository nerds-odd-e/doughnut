<template>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <div v-if="noteViewedByUser">
      <NoteViewedByUser v-bind="noteViewedByUser"/>
      <NoteStatisticsButton :noteid="noteid"/>
    </div>
  </LoadingPage>
</template>

<script setup>
import NoteViewedByUser from "../components/notes/NoteViewedByUser.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import LoadingPage from "./LoadingPage.vue"
import {restGet} from "../restful/restful"
import { ref, watch, defineProps } from "vue"

const props = defineProps({noteid: Number})
const noteViewedByUser = ref(null)
const loading = ref(false)

const fetchData = async () => {
  restGet(`/api/notes/${props.noteid}`, loading, (res) => noteViewedByUser.value = res)
}

watch(()=>props.noteid, ()=>fetchData())
fetchData()
</script>
