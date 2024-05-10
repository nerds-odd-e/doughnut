<template>
  <NoteShell v-if="note" v-bind="{ id: note.id, updatedAt: note.updatedAt }">
    <NoteFrameOfLinks v-if="links" v-bind="{ links, storageAccessor }">
      <div class="alert alert-warning" v-if="note.deletedAt">
        This note has been deleted
      </div>
      <NoteTextContent :note="note" :storage-accessor="storageAccessor">
        <template #topic-additional>
          <div class="header-options">
            <NoteWikidataAssociation
              v-if="note.wikidataId"
              :wikidata-id="note.wikidataId"
            />
          </div>
        </template>
      </NoteTextContent>
      <NoteAccessoryAsync
        :note-id="note.id"
        :note-accessory="note.noteAccessory"
      />
    </NoteFrameOfLinks>
    <template #footer>
      <slot name="footer" />
    </template>
  </NoteShell>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteAccessoryAsync from "./NoteAccessoryAsync.vue";
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import NoteShell from "./NoteShell.vue";
import NoteTextContent from "./NoteTextContent.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import LinksMap from "../../models/LinksMap";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    links: {
      type: Object as PropType<LinksMap>,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteAccessoryAsync,
    NoteWikidataAssociation,
    NoteTextContent,
  },
});
</script>

<style scoped>
.note-topic {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>
