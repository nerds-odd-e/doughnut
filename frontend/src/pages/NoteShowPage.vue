<template>
  <LoadingPage v-bind="{ loading, contentExists: !!noteViewedByUser }">
    <div v-if="noteViewedByUser">
      <NoteViewedByUser v-bind="{...noteViewedByUser, noteId: noteViewedByUser.id}" @updated="fetchData()" />
      <NoteStatisticsButton :noteId="noteId" />
    </div>
  </LoadingPage>
</template>

<script>
import NoteViewedByUser from "../components/notes/NoteViewedByUser.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import { restGet } from "../restful/restful";

export default {
  name: "NoteShowPage",
  props: { noteId: Number },
  data() {
    return {
      noteViewedByUser: null,
      loading: false,
    };
  },
  components: { NoteViewedByUser, NoteStatisticsButton, LoadingPage },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteId}`, (r) => (this.loading = r)).then(
        (res) => {
          this.$store.commit('loadNotes', [res])
          this.noteViewedByUser = res
        }
      );
    },
  },
  watch: {
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
