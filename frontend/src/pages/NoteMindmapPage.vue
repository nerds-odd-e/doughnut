<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <div v-if="!loading">
      <Breadcrumb v-bind="notePosition" />
      <NoteOverview v-bind="{ noteId }" />
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NoteOverview from "../components/notes/NoteOverview.vue";
import Breadcrumb from "../components/notes/Breadcrumb.vue";

export default {
  name: "NoteOverviewPage",
  props: { noteId: Number },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { LoadingPage, NoteOverview, Breadcrumb },
  methods: {

    fetchData() {
      this.loading = true;
      storedApiGetNoteWithDescendents(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      })
      .finally(() => this.loading = false)
      ;
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
