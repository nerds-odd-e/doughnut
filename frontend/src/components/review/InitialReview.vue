<template>
  <ContainerPage v-bind="{ contentExists: true }">
    <main>
      <NoteWithBreadcrumb v-bind="{ note, storageAccessor }" />
    </main>
    <NoteInfoBar
      :note-id="note.id"
      :key="note.id"
      @level-changed="$emit('reloadNeeded', $event)"
    />
    <InitialReviewButtons
      :key="buttonKey"
      @do-initial-review="processForm($event)"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import ContainerPage from "@/pages/commons/ContainerPage.vue";
import NoteInfoBar from "../notes/NoteInfoBar.vue";
import InitialReviewButtons from "./InitialReviewButtons.vue";
import usePopups from "../commons/Popups/usePopups";
import NoteWithBreadcrumb from "./NoteWithBreadcrumb.vue";

export default defineComponent({
  name: "InitialReview",
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["reloadNeeded", "initialReviewDone"],
  components: {
    NoteWithBreadcrumb,
    ContainerPage,
    NoteInfoBar,
    InitialReviewButtons,
  },
  computed: {
    buttonKey() {
      return this.note.id;
    },
  },

  methods: {
    async processForm(skipReview: boolean) {
      if (skipReview) {
        if (
          !(await this.popups.confirm(
            "Confirm to hide this note from reviewing in the future?",
          ))
        )
          return;
      }
      this.managedApi.restReviewsController
        .create({
          noteId: this.note.id,
          skipReview,
        })
        .then((data) => {
          if (skipReview) {
            this.$emit("reloadNeeded", data);
          } else {
            this.$emit("initialReviewDone", data);
          }
        });
    },
  },
});
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
