<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="selectedNote"
    :note="selectedNote"
    :viewType="viewType"
    :featureToggle="featureToggle"
    />
    <div class="btn-group btn-group-sm">
      <LinkNoteButton :note="selectedNote" />
      <NoteUndoButton/>
    </div>
  </nav>
  <Breadcrumb v-bind="selectedNotePosition"/>
</template>

<script lang="ts">

import { defineComponent, PropType } from 'vue'
import Breadcrumb from "./Breadcrumb.vue";
import NoteButtons from './NoteButtons.vue'
import NoteUndoButton from "./NoteUndoButton.vue";
import LinkNoteButton from "../links/LinkNoteButton.vue";
import useStoredLoadingApi from '../../managedApi/useStoredLoadingApi';

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    selectedNote: Object as PropType<Generated.Note>,
    selectedNotePosition: Object as PropType<Generated.NotePositionViewedByUser>,
  },
  components: {
    NoteButtons,
    NoteUndoButton,
    LinkNoteButton,
    Breadcrumb,
  },
  computed: {
    viewType() { return this.piniaStore.viewType },
    featureToggle() { return this.piniaStore.featureToggle },
  },
});
</script>
