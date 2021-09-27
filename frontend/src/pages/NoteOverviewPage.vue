<template>
  <LoadingPage v-bind="{ loading, contentExists: !!breadcrumb }">
    <div v-if="!loading">
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
      loading: true,
    };
  },
  components: { LoadingPage, NoteOverview, Breadcrumb },
  methods: {

    fetchData() {
      this.loading = true;
      storedApiGetNoteWithDescendents(this.$store, this.noteId)
      .then((res) => {
        this.breadcrumb = res.noteBreadcrumbViewedByUser;
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
