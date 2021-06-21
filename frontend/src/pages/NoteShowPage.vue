<template>
  <LoadingThinBar v-if="loading"/>
  <div v-if="noteViewedByUser">
    <NoteViewedByUser v-bind="noteViewedByUser"/>
  </div>
  <div v-else><ContentLoader /></div>

</template>

<script setup>
import NoteViewedByUser from "../components/notes/NoteViewedByUser.vue"
import ContentLoader from "../components/ContentLoader.vue"
import LoadingThinBar from "../components/LoadingThinBar.vue"
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
