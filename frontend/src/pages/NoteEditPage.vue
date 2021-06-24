<template>
  <h2>Edit Note</h2>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <div v-if="!!noteViewedByUser">
        <NoteOwnerBreadcrumb v-bind="noteViewedByUser">
            <li class="breadcrumb-item">{{noteViewedByUser.note.title}}</li>
        </NoteOwnerBreadcrumb>
        <form :action="`/notes/${noteViewedByUser.note.id}`" method="post" enctype="multipart/form-data">
            <NoteFormBody v-model="noteFormData"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
    </div>
  </LoadingPage>
</template>

<script setup>
import NoteOwnerBreadcrumb from "../components/notes/NoteOwnerBreadcrumb.vue"
import NoteFormBody from "../components/notes/NoteFormBody.vue"
import LoadingPage from "./LoadingPage.vue"
import {restGet} from "../restful/restful"
import { computed, ref, watch, defineProps } from "vue"

const props = defineProps({noteid: Number})
const noteViewedByUser = ref(null)
const loading = ref(false)
const noteFormData = computed(()=>noteViewedByUser.value.note.noteContent)

const fetchData = async () => {
  restGet(`/api/notes/${props.noteid}`, loading, (res) => noteViewedByUser.value = res)
}

watch(()=>props.noteid, ()=>fetchData())
fetchData()
</script>
