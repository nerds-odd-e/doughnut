<template>
  <h2>Edit Note</h2>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <div v-if="!!noteViewedByUser">
        <NoteOwnerBreadcrumb v-bind="noteViewedByUser">
            <li class="breadcrumb-item">{{noteViewedByUser.note.title}}</li>
        </NoteOwnerBreadcrumb>
        <form @submit.prevent="processForm">
            <NoteFormBody v-model="noteFormData" :errors="noteFormErrors"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
    </div>
  </LoadingPage>
</template>

<script setup>
import NoteOwnerBreadcrumb from "../components/notes/NoteOwnerBreadcrumb.vue"
import NoteFormBody from "../components/notes/NoteFormBody.vue"
import LoadingPage from "./LoadingPage.vue"
import {restGet, restPostMultiplePartForm} from "../restful/restful"
import { computed, ref, watch, defineProps } from "vue"

const props = defineProps({noteid: Number})
const emit = defineEmit(['redirect'])
const noteViewedByUser = ref(null)
const loading = ref(false)
const noteFormErrors = ref()
const noteFormData = computed(()=>{
  const {updatedAt, ...rest} = noteViewedByUser.value.note.noteContent
  return rest
})

const fetchData = () => {
  restGet(`/api/notes/${props.noteid}`, loading, (res) => noteViewedByUser.value = res)
}

const processForm = () => {
  restPostMultiplePartForm(
    `/api/notes/${props.noteid}`,
    noteFormData.value,
    loading,
    (res) => emit("redirect", {name: "noteShow", params: { noteid: res.noteId}}),
    (res) => noteFormErrors.value = res,
  )
}

watch(()=>props.noteid, ()=>fetchData())
fetchData()
</script>
