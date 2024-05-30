<template>
  <div class="row">
    <NoteCoreToolbar
      v-if="!readonly"
      v-bind="{ note: noteRealm.note, storageAccessor }"
      @note-accessory-updated="updatedNoteAccessory = $event"
    />
  </div>

  <div class="row">
    <div id="main-note-content" class="col-md-9">
      <NoteTextContent
        v-bind="{
          note: noteRealm.note,
          links: noteRealm.links,
          readonly,
          storageAccessor,
        }"
      />
      <NoteAccessoryAsync
        v-bind="{ noteId: noteRealm.id, updatedNoteAccessory, readonly }"
      />
      <NoteRecentUpdateIndicator
        v-bind="{
          id: noteRealm.id,
          updatedAt: noteRealm.note.updatedAt,
        }"
      >
        <p>
          <span class="me-3">
            Created: {{ toLocalDateString(noteRealm.note.createdAt) }}
          </span>
          <span>
            Last updated: {{ toLocalDateString(noteRealm.note.updatedAt) }}
          </span>
        </p>
      </NoteRecentUpdateIndicator>
      <ChildrenNotes
        v-bind="{ expandChildren, readonly, storageAccessor }"
        :notes="noteRealm.children ?? []"
      />
    </div>
    <div class="col-md-3 refers" v-if="noteRealm.links">
      <ul>
        <template
          v-for="(linksOfType, linkType) in noteRealm.links"
          :key="linkType"
        >
          <li v-if="linksOfType && linksOfType.reverse.length > 0">
            <span>{{ reverseLabel(linkType) }} </span>
            <LinkOfNote
              class="link-multi"
              v-for="link in linksOfType.reverse"
              :key="link.id"
              v-bind="{ note: link, storageAccessor }"
              :reverse="true"
            />
          </li>
        </template>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { NoteRealm, NoteAccessory } from "@/generated/backend";
// eslint-disable-next-line import/no-unresolved
import NoteTextContent from "./core/NoteTextContent.vue";
import ChildrenNotes from "./ChildrenNotes.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue";
import NoteCoreToolbar from "./core/NoteCoreToolbar.vue";
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue";
import LinkOfNote from "../links/LinkOfNote.vue";
import { reverseLabel } from "../../models/linkTypeOptions";

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

const toLocalDateString = (date: string) => {
  return new Date(date).toLocaleDateString();
};
</script>

<style scoped>
.refers {
  border-left: 1px solid #e9ecef;
}
</style>
