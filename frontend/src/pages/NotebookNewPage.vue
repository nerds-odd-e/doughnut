<template>
  <h2>Adding new notebook</h2>
  <LoadingPage v-bind="{loading, contentExists: true}">
    <div v-if="true">
        <NoteBreadcrumbForOwnOrCircle v-bind="{ancestors: []}">
            <li class="breadcrumb-item">(adding here)</li>
        </NoteBreadcrumbForOwnOrCircle>
        <form @submit.prevent="processForm">
            <NoteFormBody v-model="noteFormData" :errors="noteFormErrors"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
    </div>
  </LoadingPage>
</template>

<script setup>
import NoteBreadcrumbForOwnOrCircle from "../components/notes/NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "../components/notes/NoteFormBody.vue"
import LoadingPage from "./LoadingPage.vue"
import {restPostMultiplePartForm} from "../restful/restful"
import { ref } from "vue"

const emit = defineEmit(['redirect'])
const loading = ref(false)
const noteFormData = ref({})
const noteFormErrors = ref()

const processForm = () => {
  restPostMultiplePartForm(
    `/api/notebooks/create`,
     noteFormData.value,
     loading,
     (res) => emit("redirect", {name: "noteShow", params: { noteid: res.noteId}}),
     (res) => noteFormErrors.value = Object.fromEntries(res.errors.map(err=>[err.field, err.defaultMessage])),
    )
}
</script>
