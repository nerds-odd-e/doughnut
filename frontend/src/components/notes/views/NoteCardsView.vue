<template>
  <LoadingPage v-bind="{ loading, contentExists: !!noteRealm }">
    <div class="inner-box" v-if="noteRealm" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="notePosition"
          v-bind="{
            selectedNote: noteRealm.note,
            selectedNotePosition: notePosition,
            viewType: 'cards',
          }"
          @note-deleted="onNoteDeleted"
          @note-realm-updated="noteRealmUpdated($event)"
          @new-note-added="newNoteAdded($event)"
        />
      </div>

      <NoteWithLinks
        v-bind="{ note: noteRealm.note, links: noteRealm.links }"
        @note-realm-updated="noteRealmUpdated($event)"
      >
        <template #footer>
          <NoteStatisticsButton
            :note-id="noteRealm.id"
            :expanded="expandInfo"
            :key="noteRealm.id"
            @level-changed="$emit('levelChanged', $event)"
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
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
    expandInfo: { type: Boolean, default: false },
  },
  emits: ["levelChanged"],
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
    LoadingPage,
    NoteToolbar,
  },
  data() {
    return {
      notePosition: undefined as Generated.NotePositionViewedByUser | undefined,
      noteRealm: undefined as Generated.NoteRealm | undefined,
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },
  methods: {
    onNoteDeleted() {
      this.$router.push({
        name: "noteShow",
        params: { noteId: this.noteRealm?.note.parentId },
      });
    },
    newNoteAdded(newNote: Generated.NoteRealmWithPosition) {
      this.$router.push({
        name: "noteShow",
        params: { noteId: newNote.notePosition.noteId },
      });
    },
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      if (!updatedNoteRealm) {
        this.fetchData();
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
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
