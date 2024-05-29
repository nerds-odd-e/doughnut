<template>
  <ContainerPage v-bind="{ contentExists: true }">
    <ShowThing v-bind="{ thing, storageAccessor }" />
    <NoteInfoBar
      v-if="thing.note"
      :note-id="thing.note.id"
      :key="thing.note.id"
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
import { Thing } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import ContainerPage from "@/pages/commons/ContainerPage.vue";
import ShowThing from "./ShowThing.vue";
import NoteInfoBar from "../notes/NoteInfoBar.vue";
import InitialReviewButtons from "./InitialReviewButtons.vue";
import usePopups from "../commons/Popups/usePopups";

export default defineComponent({
  name: "InitialReview",
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    thing: {
      type: Object as PropType<Thing>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["reloadNeeded", "initialReviewDone"],
  components: {
    ShowThing,
    ContainerPage,
    NoteInfoBar,
    InitialReviewButtons,
  },
  computed: {
    buttonKey() {
      return this.thing.note?.id;
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
          noteId: this.thing.note!.id,
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
