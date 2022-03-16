<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :viewType="viewType"
    :featureToggle="featureToggle"
    />
    <div class="btn-group btn-group-sm">
      <LinkNoteButton :note="currentNote" />
      <NoteUndoButton/>
    </div>
  </nav>
  <Breadcrumb v-bind="notePosition"/>
</template>

<script lang="ts">

import { defineComponent } from 'vue'
import Breadcrumb from "./Breadcrumb.vue";
import NoteButtons from './NoteButtons.vue'
import NoteUndoButton from "./NoteUndoButton.vue";
import LinkNoteButton from "../links/LinkNoteButton.vue";
import useStoredLoadingApi from '../../managedApi/useStoredLoadingApi';

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  components: {
    NoteButtons,
    NoteUndoButton,
    LinkNoteButton,
    Breadcrumb,
  },
  computed: {
    currentNote() { return this.piniaStore.getHighlightNote() },
    viewType() { return this.piniaStore.viewType },
    featureToggle() { return this.piniaStore.featureToggle },
    notePosition(): Generated.NotePositionViewedByUser | undefined {
      return this.piniaStore.getHighlightNotePosition()
    },
  },
});
</script>
