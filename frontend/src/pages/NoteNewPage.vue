<template>
  <h2>Adding new note</h2>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <div v-if="!!noteViewedByUser">
        <NoteBreadcrumbForOwnOrCircle v-bind="{...noteViewedByUser, ancestors: [...noteViewedByUser.ancestors, noteViewedByUser.note]}">
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
import LoadingPage from "./LoadingPage.vue"
import {restGet, restPostMultiplePartForm} from "../restful/restful"

export default {
  name: 'NoteNewPage',
  components: {NoteBreadcrumbForOwnOrCircle, NoteFormBody, LoadingPage},
  props: {noteid: Number},
  data() {
    return {
      noteViewedByUser: null,
      loading: false,
      noteFormErrors: {},
      noteFormData: {}
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteid}`, r=>this.loading=r, (res) => this.noteViewedByUser = res)
    },

    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.noteid}/create`,
        this.noteFormData,
        r=>this.loading=r,
        (res) => this.$router.push({name: "noteShow", params: { noteid: res.noteId}}),
        (res) => this.noteFormErrors = res,
      )
    }
  },
  mounted() {
    this.fetchData()

  }
}
</script>
