<template>
  <h2>Adding new note</h2>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <div v-if="!!noteViewedByUser">
        <NoteBreadcrumbForOwnOrCircle v-bind="{...noteViewedByUser, ancestors: [...noteViewedByUser.ancestors, noteViewedByUser.note]}">
            <li class="breadcrumb-item">(adding here)</li>
        </NoteBreadcrumbForOwnOrCircle>
        <form @submit.prevent="processForm">
            <NoteFormBody v-model="noteFormData"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
    </div>
  </LoadingPage>
</template>

<script setup>
import NoteBreadcrumbForOwnOrCircle from "../components/notes/NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "../components/notes/NoteFormBody.vue"
import LoadingPage from "./LoadingPage.vue"
import {restGet, restPostMultiplePartForm} from "../restful/restful"
import { computed, ref, watch, defineProps } from "vue"

const props = defineProps({noteid: Number})
const emit = defineEmit(['redirect'])
const noteViewedByUser = ref(null)
const loading = ref(false)
const noteFormData = ref({})

const fetchData = () => {
  restGet(`/api/notes/${props.noteid}`, loading, (res) => noteViewedByUser.value = res)
}

const processForm = () => {
  restPostMultiplePartForm(`/api/notes/${props.noteid}/create`, noteFormData.value, loading, (res) => emit("redirect", {name: "noteShow", params: { noteid: res.noteId}}))
}

watch(()=>props.noteid, ()=>fetchData())
fetchData()
</script>
