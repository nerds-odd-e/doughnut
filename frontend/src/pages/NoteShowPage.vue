<template>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
    <div v-if="noteViewedByUser">
      <NoteViewedByUser v-bind="noteViewedByUser" @updated="fetchData()"/>
      <NoteStatisticsButton :noteid="noteid"/>
    </div>
  </LoadingPage>
</template>

<script>
import NoteViewedByUser from "../components/notes/NoteViewedByUser.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet} from "../restful/restful"

export default {
  name: "NoteShowPage",
  props: {noteid: Number},
  data() {
    return {
      noteViewedByUser: null,
      loading: false,
    }
  },
  components: { NoteViewedByUser, NoteStatisticsButton, LoadingPage },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteid}`, (r)=>this.loading=r, (res) => this.noteViewedByUser = res)
    }
  },
  watch: {
    noteid() {
      this.fetchData()
    }
  },
  mounted() {
    this.fetchData()
  }
}

</script>
