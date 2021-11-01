<template>
    <div class="box">
      <div class="header">
        <NoteControl :noteId="highlightNoteId" :deleteRedirect="false"/>
        <Breadcrumb v-bind="notePosition" :noteRouteName="`mindmap`"/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, highlightNoteId}"
          @highlight="highlight"
        />
      </div>
    </div>
</template>

<script>
import NoteControl from "../commons/NoteControl.vue";
import NoteMindmapWithListner from "./NoteMindmapWithListner.vue";
import Breadcrumb from "./Breadcrumb.vue";

export default {
  props: {
     noteId: [String, Number],
     notePosition: Object,
     noteComponent: String,
  },
  data() {
    return {
      highlightNoteId: null,
    };
  },
  components: { NoteControl, NoteMindmapWithListner, Breadcrumb },
  methods: {
    highlight(id) { this.highlightNoteId = id},
  },
  watch: {
    noteId() {
      this.highlightNoteId = this.noteId
    },
  },
  mounted() {
    this.highlightNoteId = this.noteId
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