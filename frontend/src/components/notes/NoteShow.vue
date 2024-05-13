<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <Breadcrumb v-bind="{ notePosition: noteRealm.notePosition }">
        <NoteNewButton
          v-if="noteRealm.note.parentId && !readonly"
          v-bind="{ parentId: noteRealm.note.parentId, storageAccessor }"
          button-title="Add Sibling Note"
        >
          <SvgAddSibling />
        </NoteNewButton>
      </Breadcrumb>
      <NoteWithLinks
        v-bind="{
          note: noteRealm.note,
          links: noteRealm.links,
          readonly,
          storageAccessor,
        }"
      />
      <NoteAccessoryAsync
        :note-id="noteRealm.id"
        :note-accessory="noteRealm.note.noteAccessory"
      />
      <NoteInfoBar
        :note-id="noteId"
        :expanded="expandInfo"
        :key="noteId"
        @level-changed="$emit('levelChanged', $event)"
        @self-evaluated="$emit('selfEvaluated', $event)"
      />
      <Cards v-if="expandChildren" :notes="noteRealm.children" />
      <slot />
      <NoteChatDialog
        v-bind="{ selectedNote: noteRealm.note, storageAccessor }"
      />
    </template>
  </NoteRealmLoader>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "./core/NoteWithLinks.vue";
import Cards from "./Cards.vue";
import NoteInfoBar from "./NoteInfoBar.vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteChatDialog from "./NoteChatDialog.vue";
import NoteAccessoryAsync from "./NoteAccessoryAsync.vue";

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
  emits: ["levelChanged", "selfEvaluated"],
  components: {
    NoteWithLinks,
    Cards,
    NoteInfoBar,
    Breadcrumb,
    NoteAccessoryAsync,
    NoteChatDialog,
  },
});
</script>
