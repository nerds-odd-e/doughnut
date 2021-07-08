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

<script>
import NoteBreadcrumbForOwnOrCircle from "../components/notes/NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "../components/notes/NoteFormBody.vue"
import LoadingPage from "./commons/LoadingPage.vue"
import {restPostMultiplePartForm} from "../restful/restful"
import { ref } from "vue"

export default {
  name: 'NotebookNewPage',
  components: { NoteBreadcrumbForOwnOrCircle, NoteFormBody, LoadingPage},
  data() {
    return {
      loading: false,
      noteFormData: {},
      noteFormErrors: {}
    }
  },
  methods: {
    processForm() {
      restPostMultiplePartForm(
        `/api/notebooks/create`,
        this.noteFormData,
        r=>this.loading=r,
        (res) => this.$router.push({name: "noteShow", params: { noteId: res.noteId}}),
        (res) => this.noteFormErrors = res,
        )
    }
  }
}
</script>
