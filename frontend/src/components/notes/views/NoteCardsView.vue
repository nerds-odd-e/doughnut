<template>
  <LoadingPage v-bind="{ loading, contentExists: !!noteRealm }">
    <div class="inner-box" v-if="noteRealm" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="notePosition"
          v-bind="{
            selectedNoteId: noteId,
            selectedNotePosition: notePosition,
            viewType: 'cards',
          }"
          @note-deleted="onNoteDeleted"
          @note-realm-updated="noteRealmUpdated($event)"
        />
      </div>

      <NoteWithLinks
        v-bind="{ note: noteRealm.note, links: noteRealm.links }"
        @note-realm-updated="noteRealmUpdated($event)"
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
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import NoteToolbar from "@/components/toolbars/NoteToolbar.vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import Cards from "../Cards.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";
import NoteInfoButton from "../NoteInfoButton.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    updatedNoteRealm: { type: Object },
    expandChildren: { type: Boolean, required: true },
    expandInfo: { type: Boolean, default: false },
  },
  emits: ["levelChanged", "noteDeleted", "selfEvaluated"],
  components: {
    NoteWithLinks,
    Cards,
    LoadingPage,
    NoteToolbar,
    NoteInfoButton,
  },
  data() {
    return {
      notePosition: undefined as Generated.NotePositionViewedByUser | undefined,
      noteRealm: undefined as Generated.NoteRealm | undefined,
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  methods: {
    onNoteDeleted() {
      if (this.noteRealm?.note.parentId) {
        this.$router.push({
          name: "noteShow",
          params: { noteId: this.noteRealm?.note.parentId },
        });
      } else {
        this.$router.push({ name: "notebooks" });
      }
      this.$emit("noteDeleted");
    },
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      if (!updatedNoteRealm) {
        this.fetchData();
        return;
      }
      if (updatedNoteRealm.id !== this.noteId) {
        this.$router.push({
          name: "noteShow",
          params: { noteId: updatedNoteRealm.id },
        });
        return;
      }
      this.noteRealm = updatedNoteRealm;
    },
    async fetchData() {
      const noteRealmWithPosition =
        await this.api.noteMethods.getNoteRealmWithPosition(this.noteId);
      this.notePosition = noteRealmWithPosition.notePosition;
      this.noteRealm = noteRealmWithPosition.noteRealm;
    },
  },
  watch: {
    updatedNoteRealm(updatedNoteRealm) {
      this.noteRealmUpdated(updatedNoteRealm);
    },
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
