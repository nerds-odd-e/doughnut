<template>
  <LoadingPage v-bind="{ loading, contentExists: !!loaded }">
    <div v-if="loaded">
      <Breadcrumb v-bind="breadcrumb" />
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
      breadcrumb: null,
      loaded: false,
      loading: false,
    };
  },
  components: { LoadingPage, NoteOverview, Breadcrumb },
  methods: {

    fetchData() {
      storedApiGetNoteWithDescendents(this.$store, this.noteId,
        (r) => (this.loading = r)
      ).then((res) => {
        this.breadcrumb = res.noteBreadcrumbViewedByUser;
        this.loaded = true;
      });
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
