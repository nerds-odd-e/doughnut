<template>
<div class="container">
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <div v-if="notePosition" :key="noteId">
      <NoteWithChildrenCards v-if="!loading" v-bind="{id: noteId, notePosition}"/>
      <NoteStatisticsButton :noteId="noteId" />
    </div>
  </LoadingPage>
</div>
</template>

<script>
import NoteWithChildrenCards from "../components/notes/NoteWithChildrenCards.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import { storedApiGetNoteAndItsChildren } from "../storedApi";

export default {
  name: "NoteShowPage",
  props: { noteId: Number },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { NoteWithChildrenCards, NoteStatisticsButton, LoadingPage },
  methods: {
    fetchData() {
      this.loading = true
      storedApiGetNoteAndItsChildren(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
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
