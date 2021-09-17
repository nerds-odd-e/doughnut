<template>
  <LoadingPage v-bind="{ loading, contentExists: !!loaded }">
    <div v-if="loaded">
      <NoteOverview v-bind="{ noteId }" />
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
      ancestors: null,
      loaded: false,
      loading: false,
    };
  },
  components: { LoadingPage, NoteOverview },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteId}/overview`, (r) => (this.loading = r)).then(
        (res) => {
          this.breadcrumb = res.breadcrumb
          this.$store.commit('loadNotes', res.notes)
          this.$store.commit('loadParentChildren', res.parentChildren)
          this.loaded = true
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
