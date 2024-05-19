<template>
  <div class="row">
    <NoteCoreToolbar
      v-if="!readonly"
      v-bind="{ note: noteRealm.note, storageAccessor }"
      @note-accessory-updated="updatedNoteAccessory = $event"
    />
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
        <NoteAccessoryAsync
          v-bind="{ noteId: noteRealm.id, updatedNoteAccessory, readonly }"
        />
        <NoteInfoBar
          :note-id="noteRealm.id"
          :expanded="false"
          :key="noteRealm.id"
        />
      </NoteRecentUpdateIndicator>
    </div>
  </div>
  <ChildrenNotes
    v-bind="{ expandChildren, readonly, storageAccessor }"
    :notes="noteRealm.children ?? []"
  />
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { NoteRealm, NoteAccessory } from "@/generated/backend";
import NoteWithLinks from "./core/NoteWithLinks.vue";
import ChildrenNotes from "./ChildrenNotes.vue";
import NoteInfoBar from "./NoteInfoBar.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue";
import NoteCoreToolbar from "./core/NoteCoreToolbar.vue";
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue";

defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
  expandChildren: { type: Boolean, required: true },
  expandInfo: { type: Boolean, default: false },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const updatedNoteAccessory = ref<NoteAccessory | undefined>(undefined);
</script>
