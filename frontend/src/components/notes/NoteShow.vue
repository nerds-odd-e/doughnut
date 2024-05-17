<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <Breadcrumb v-bind="{ notePosition: noteRealm.notePosition }" />
      <div class="row">
        <div class="col-md-8 d-flex flex-column p-0">
          <NoteWithLinks
            v-bind="{
              note: noteRealm.note,
              links: noteRealm.links,
              readonly,
              storageAccessor,
            }"
          />
        </div>
        <div class="col-md-4 d-flex flex-column p-0">
          <NoteRecentUpdateIndicator
            v-bind="{
              id: noteRealm.id,
              updatedAt: noteRealm.note.updatedAt,
            }"
          >
            <NoteAccessoryAsync v-bind="{ noteId: noteRealm.id, readonly }" />
            <NoteInfoBar
              :note-id="noteId"
              :expanded="expandInfo"
              :key="noteId"
              @level-changed="$emit('levelChanged', $event)"
            />
          </NoteRecentUpdateIndicator>
        </div>
      </div>
      <ChildrenNotes
        v-bind="{ expandChildren, storageAccessor }"
        :notes="noteRealm.children"
      />
      <slot />
    </template>
  </NoteRealmLoader>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "./core/NoteWithLinks.vue";
import ChildrenNotes from "./ChildrenNotes.vue";
import NoteInfoBar from "./NoteInfoBar.vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue";
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
    expandInfo: { type: Boolean, default: false },
    readonly: { type: Boolean, default: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["levelChanged"],
  components: {
    NoteWithLinks,
    ChildrenNotes,
    NoteInfoBar,
    Breadcrumb,
    NoteAccessoryAsync,
    NoteRecentUpdateIndicator,
  },
});
</script>
