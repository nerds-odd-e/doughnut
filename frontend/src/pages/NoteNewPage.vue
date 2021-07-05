<template>
  <h2>Adding new note</h2>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <NoteNew v-if="!!noteViewedByUser" :ancestors="[...noteViewedByUser.ancestors, noteViewedByUser.note]" :notebook="noteViewedByUser.notebook"/>
  </LoadingPage>
</template>

<script>
import NoteNew from "../components/notes/NoteNewButton.vue"
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet} from "../restful/restful"

export default {
  name: 'NoteNewPage',
  components: {NoteNew, LoadingPage},
  props: {noteid: Number},
  data() {
    return {
      noteViewedByUser: null,
      loading: false,
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteid}`, r=>this.loading=r, (res) => this.noteViewedByUser = res)
    },

  },
  mounted() {
    this.fetchData()

  }
}
</script>
