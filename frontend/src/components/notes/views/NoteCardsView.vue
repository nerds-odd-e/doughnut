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
import NoteInfoButton from "../NoteInfoButton.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  setup(props) {
    return { noteRealm: props.storageAccessor.refOfNoteRealm(props.noteId) };
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
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  methods: {
    async fetchData() {
      this.noteRealm = await this.storageAccessor
        .api(this.$router)
        .getNoteRealmAndReloadPosition(this.noteId);
    },
  },
  watch: {
    "storageAccessor.storageUpdatedAt": function updateAt() {
      if (this.storageAccessor.updatedNoteRealm?.id === this.noteId) {
        this.noteRealm = this.storageAccessor.updatedNoteRealm;
      }
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
