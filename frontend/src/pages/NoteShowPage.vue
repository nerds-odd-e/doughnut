<template>
    <NoteSphereAsync
      v-bind="{
        noteId,
        viewType,
        expandChildren: true
      }"
      :key="noteId"
      />
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import NoteSphereAsync from '../components/notes/NoteSphereAsync.vue';

export default defineComponent({
  props: { rawNoteId: String, viewType: String },
  components: { NoteSphereAsync },
  computed: {
    noteId(): number {
      if(!this.rawNoteId) return Number.NaN;
      return Number.parseInt(this.rawNoteId)
    },
  },
});

</script>