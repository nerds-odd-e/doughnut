<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notePosition }">
    <div v-if="notePosition" :key="noteId">
      <NoteWithChildrenCards v-if="!loading" v-bind="{id: noteId, notePosition}"/>
      <NoteStatisticsButton :noteId="noteId" />
    </div>
  </ContainerPage>
</template>

<script>
import NoteWithChildrenCards from "../components/notes/NoteWithChildrenCards.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import { storedApiGetNoteAndItsChildren } from "../storedApi";

export default {
  name: "NoteShowPage",
  props: { noteId: [String, Number] },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { NoteWithChildrenCards, NoteStatisticsButton, ContainerPage },
  methods: {
    fetchData() {
      this.loading = true
      storedApiGetNoteAndItsChildren(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
        this.$store.commit('highlightNoteId', this.noteId)
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
  unmounted() {
    this.$store.commit('highlightNoteId', null)
  }
};
</script>
