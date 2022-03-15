<template>
    <div class="inner-box">
      <div class="header">
        <NoteControl/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, expandChildren}"
        />
      </div>
    </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from 'vue'
import NoteControl from "../../toolbars/NoteControl.vue";
import NoteMindmapView from "./NoteMindmapView.vue";
import NoteCardsView from "./NoteCardsView.vue";
import NoteArticleView from "./NoteArticleView.vue";
import useStoredLoadingApi from '../../../managedApi/useStoredLoadingApi';


export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
     noteId: { type: Number, required: true },
     noteComponent: String,
     expandChildren: { type: Boolean, required: true },
  },
  components: { NoteControl, NoteMindmapView, NoteCardsView, NoteArticleView },
  methods: {
    highlight(id: Doughnut.ID) { 
      this.piniaStore.setHighlightNoteId(id)
    },
  },
  watch: {
    noteId() {
      this.highlight(this.noteId)
    },
  },
  mounted() {
    this.highlight(this.noteId)
  }
});
</script>
