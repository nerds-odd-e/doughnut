<template>
  <LoadingPage v-bind="{ contentExists: !!noteRealm }">
    <div class="inner-box" v-if="noteRealm" :key="noteId">
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
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import Cards from "../Cards.vue";
import useLoadingApi from "../../../managedApi/useLoadingApi";
import NoteInfoButton from "../NoteInfoButton.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
    expandInfo: { type: Boolean, default: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["levelChanged", "selfEvaluated"],
  components: {
    NoteWithLinks,
    Cards,
    LoadingPage,
    NoteInfoButton,
  },
  data() {
    return {
      noteRealm: undefined as Generated.NoteRealm | undefined,
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  methods: {
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      if (!updatedNoteRealm) {
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
      const noteRealmWithPosition = await this.storageAccessor
        .api()
        .getNoteRealmWithPosition(this.noteId);
      this.noteRealm = noteRealmWithPosition.noteRealm;
    },
  },
  watch: {
    "storageAccessor.storageUpdatedAt": function updateAt() {
      this.noteRealmUpdated(this.storageAccessor.updatedNoteRealm);
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
