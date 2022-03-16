<template>
    <div class="inner-box">
      <div class="header">
        <NoteControl v-bind="{selectedNoteId}"/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, expandChildren}"
          @selectNote="highlight($event)"
        />
      </div>
    </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
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
  data() {
    return {
      selectedNoteId: undefined
    } as {
      selectedNoteId: Doughnut.ID | undefined
    }
  },
  components: { NoteControl, NoteMindmapView, NoteCardsView, NoteArticleView },
  methods: {
    highlight(id: Doughnut.ID) { 
      this.selectedNoteId = id
    },
  },
  mounted() {
    this.highlight(this.noteId)
  }
});
</script>
