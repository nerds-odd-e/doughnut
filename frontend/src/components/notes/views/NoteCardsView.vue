<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <div class="inner-box" v-if="noteRealm" :key="noteId">
        <BreadcrumbMain v-bind="{ storageAccessor, readonly }" />
        <NoteWithLinks
          v-bind="{
            note: noteRealm.note,
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
        <Cards v-if="expandChildren" :notes="noteRealm.children" />
      </div>
    </template>
  </NoteRealmLoader>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import Cards from "../Cards.vue";
import NoteInfoButton from "../NoteInfoButton.vue";
import BreadcrumbMain from "../../toolbars/BreadcrumbMain.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";

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
    BreadcrumbMain,
  },
});
</script>
