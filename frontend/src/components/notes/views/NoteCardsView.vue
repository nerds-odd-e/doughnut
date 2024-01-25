<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <div v-if="noteRealm" :key="noteId">
        <Breadcrumb v-bind="noteRealm.notePosition">
          <NoteNewButton
            v-if="noteRealm.note.parentId && !readonly"
            v-bind="{ parentId: noteRealm.note.parentId, storageAccessor }"
            button-title="Add Sibling Note"
          >
            <SvgAddSibling />
          </NoteNewButton>
        </Breadcrumb>
        <ControlCenterForNote
          v-if="!readonly"
          v-bind="{ note: noteRealm.note, storageAccessor }"
        />
        <NoteWithLinks
          v-bind="{
            note: noteRealm.note,
            parentNote: getParent(noteRealm),
            links: noteRealm.links,
            storageAccessor,
          }"
        >
          <template #footer>
            <NoteInfoButton
              :note-id="noteId"
              :expanded="expandInfo"
              :key="noteId"
              @level-changed="$emit('levelChanged', $event)"
              @self-evaluated="$emit('selfEvaluated', $event)"
            />
          </template>
        </NoteWithLinks>
        <Cards
          v-if="expandChildren"
          :notes="noteRealm.children"
          :parent-note="noteRealm.note"
        />
        <slot />
        <NoteChatDialog
          v-bind="{ selectedNote: noteRealm.note, storageAccessor }"
        />
      </div>
    </template>
  </NoteRealmLoader>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import Cards from "../Cards.vue";
import NoteInfoButton from "../NoteInfoButton.vue";
import Breadcrumb from "../../toolbars/Breadcrumb.vue";
import ControlCenterForNote from "../../toolbars/ControlCenterForNote.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";
import NoteChatDialog from "../NoteChatDialog.vue";

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
    NoteInfoButton,
    Breadcrumb,
    ControlCenterForNote,
    NoteChatDialog,
  },
  methods: {
    getParent(noteRealm: Generated.NoteRealm) {
      if (noteRealm.notePosition.ancestors.length === 0) {
        return undefined;
      }
      return noteRealm.notePosition.ancestors[
        noteRealm.notePosition.ancestors.length - 1
      ];
    },
  },
});
</script>
