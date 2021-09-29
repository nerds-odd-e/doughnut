<template>
  <LoadingPage v-bind="{ loading, contentExists: !!breadcrumb }">
    <div v-if="breadcrumb" :key="noteId">
      <NoteViewedByUser v-bind="{id: noteId, breadcrumb}"/>
      <NoteStatisticsButton :noteId="noteId" />
    </div>
  </LoadingPage>
</template>

<script>
import NoteViewedByUser from "../components/notes/NoteViewedByUser.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import { storedApiGetNoteAndItsChildren } from "../storedApi";

export default {
  name: "NoteShowPage",
  props: { noteId: Number },
  data() {
    return {
      breadcrumb: null,
      loading: false,
    };
  },
  components: { NoteViewedByUser, NoteStatisticsButton, LoadingPage },
  methods: {
    fetchData() {
      this.loading = true
      storedApiGetNoteAndItsChildren(this.$store, this.noteId)
      .then((res) => {
        this.breadcrumb = res.noteBreadcrumbViewedByUser;
      }).finally(() => this.loading = false);
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
