<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notePosition }">
    <div class="box">
      <div class="header">
    <NoteControl :noteId="noteId" :deleteRedirect="true"/>
    <Breadcrumb v-bind="notePosition" />
      </div>
      <div class="content">
    <NoteWithChildrenCards v-if="!loading" v-bind="{id: noteId, owns: notePosition.owns}"/>
      </div>
    </div>
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

<style lang="sass" scoped>
.box
  display: flex
  flex-flow: column
  height: 100%

.box .header
  flex: 0 1 auto

.box .content
  flex: 1 1 auto
  overflow: hidden

.box .footer
  flex: 0 1 40px
</style>