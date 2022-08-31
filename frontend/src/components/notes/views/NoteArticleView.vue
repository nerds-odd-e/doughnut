<template>
  <template v-if="noteRealm">
    <NoteWithLinks
      v-bind="{ note: noteRealm.note, links: noteRealm.links, storageAccessor }"
    />
    <div class="note-list">
      <NoteArticleView
        v-for="child in children"
        v-bind="{ noteId: child.id, noteRealms, storageAccessor }"
        :key="child.id"
      />
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import { NoteRealmsReader } from "../../../store/NoteRealmCache";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteRealms: {
      type: Object as PropType<NoteRealmsReader>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteWithLinks },
  computed: {
    noteRealm() {
      return this.noteRealms.getNoteRealmById(this.noteId);
    },
    children() {
      return this.noteRealm?.children
        .map((child) => this.noteRealms.getNoteRealmById(child.id))
        .filter((child): child is Generated.NoteRealm => !!child);
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
