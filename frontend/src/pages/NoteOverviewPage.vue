<template>
  <LoadingPage v-bind="{ loading, contentExists: !!noteViewedByUser }">
    <div v-if="noteViewedByUser">
      <NoteOverview v-bind="noteViewedByUser" />
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import { restGet } from "../restful/restful";
import NoteOverview from "../components/notes/NoteOverview.vue";

export default {
  name: "NoteOverviewPage",
  props: { noteId: Number },
  data() {
    return {
      noteViewedByUser: null,
      loading: false,
    };
  },
  components: { LoadingPage, NoteOverview },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteId}`, (r) => (this.loading = r)).then(
        (res) => (this.noteViewedByUser = res)
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
