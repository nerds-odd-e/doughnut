<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notePosition }">
    <NoteControl :noteId="noteId"/>
    <Breadcrumb v-bind="notePosition" />
    <NoteWithChildrenCards v-if="!loading" v-bind="{id: noteId, owns: notePosition.owns}"/>
    <NoteStatisticsButton :noteId="noteId" />
  </ContainerPage>
</template>

<script>
import Breadcrumb from "../components/notes/Breadcrumb.vue";
import NoteWithChildrenCards from "../components/notes/NoteWithChildrenCards.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import NoteControl from "../components/commons/NoteControl.vue";
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
  components: { NoteControl, Breadcrumb, NoteWithChildrenCards, NoteStatisticsButton, ContainerPage },
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
