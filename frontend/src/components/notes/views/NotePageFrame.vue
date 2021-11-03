<template>
    <div class="box">
      <div class="header">
        <NoteControl :noteId="highlightNoteId" :viewType="viewType"/>
        <Breadcrumb v-bind="notePosition"/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, highlightNoteId, expandChildren}"
          @highlight="highlight"
        />
      </div>
    </div>
</template>

<script>
import NoteControl from "../../toolbars/NoteControl.vue";
import NoteMindmapView from "./NoteMindmapView.vue";
import NoteCardsView from "./NoteCardsView.vue";
import NoteArticleView from "./NoteArticleView.vue";
import Breadcrumb from "../Breadcrumb.vue";

export default {
  props: {
     noteId: [String, Number],
     notePosition: Object,
     noteComponent: String,
     viewType: String,
     expandChildren: { type: Boolean, required: true },
  },
  data() {
    return {
      highlightNoteId: null,
    };
  },
  components: { NoteControl, NoteMindmapView, Breadcrumb, NoteCardsView, NoteArticleView },
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