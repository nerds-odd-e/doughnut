<template>
  <template v-if="noteRealm">
    <NoteWithLinks
      v-bind="{ note: noteRealm.note, links: noteRealm.links }"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
    />
    <div class="note-list">
      <NoteArticleView
        v-for="child in children"
        v-bind="{ noteId: child.id, noteRealms, expandChildren }"
        :key="child.id"
        @note-realm-updated="$emit('noteRealmUpdated', $event)"
      />
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "../NoteWithLinks.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteRealms: {
      type: Object as PropType<{ [id: Doughnut.ID]: Generated.NoteRealm }>,
      required: true,
    },
    expandChildren: { type: Boolean, required: true },
  },
  components: { NoteWithLinks },
  emits: ["noteRealmUpdated"],
  computed: {
    noteRealm() {
      return this.noteRealms[this.noteId];
    },
    children() {
      return this.noteRealm.children.map((child) => this.noteRealms[child.id]);
    },
  },
});
</script>

<style lang="sass" scoped>

:deep(.note-body)
  border-width: 0px
  border-left-width: 3px

.note-list
  margin-left: 10px
</style>
