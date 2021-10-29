<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notePosition }">
    <CurrentNoteContainer :noteId="noteId" />
    <Breadcrumb v-bind="notePosition" />
    <NoteOverview v-bind="{ noteId }" />
  </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import CurrentNoteContainer from "../components/commons/CurrentNoteContainer.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NoteOverview from "../components/notes/NoteOverview.vue";
import Breadcrumb from "../components/notes/Breadcrumb.vue";

export default {
  name: "NoteOverviewPage",
  props: { noteId: String },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { CurrentNoteContainer, ContainerPage, NoteOverview, Breadcrumb },
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
