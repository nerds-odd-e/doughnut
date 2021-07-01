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

<script>
import NoteOwnerBreadcrumb from "../components/notes/NoteOwnerBreadcrumb.vue"
import NoteFormBody from "../components/notes/NoteFormBody.vue"
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet, restPostMultiplePartForm} from "../restful/restful"

export default {
  name: 'NoteEditPage',
  components: { NoteOwnerBreadcrumb, NoteFormBody, LoadingPage},
  props: { noteid: Number},
  data() {
    return {
      noteViewedByUser: null,
      loading: false,
      noteFormErrors: {}
    }
  },
  computed: {
    noteFormData(){
      const {updatedAt, ...rest} = this.noteViewedByUser.note.noteContent
      return rest
    },
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteid}`, r=>this.loading=r, (res) => this.noteViewedByUser = res)
    },

    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.noteid}`,
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
