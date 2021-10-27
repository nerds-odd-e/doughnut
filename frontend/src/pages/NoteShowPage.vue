<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notePosition }">
    <CurrentNoteContainer :noteId="noteId" v-if="notePosition" :key="noteId">
      <NoteWithChildrenCards v-if="!loading" v-bind="{id: noteId, notePosition}"/>
      <NoteStatisticsButton :noteId="noteId" />
    </CurrentNoteContainer>
  </ContainerPage>
</template>

<script>
import NoteWithChildrenCards from "../components/notes/NoteWithChildrenCards.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import CurrentNoteContainer from "../components/commons/CurrentNoteContainer.vue";
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
  components: { CurrentNoteContainer, NoteWithChildrenCards, NoteStatisticsButton, ContainerPage },
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
