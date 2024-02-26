<template>
  <ContainerPage v-bind="{ contentExists: true }">
    <ShowThing
      v-bind="{ thing, expandInfo: true, storageAccessor }"
      @level-changed="$emit('reloadNeeded', $event)"
    >
      <slot />
      <InitialReviewButtons
        :key="buttonKey"
        @do-initial-review="processForm($event)"
      />
    </ShowThing>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import ShowThing from "./ShowThing.vue";
import InitialReviewButtons from "./InitialReviewButtons.vue";
import ContainerPage from "../../pages/commons/ContainerPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";
import { StorageAccessor } from "../../store/createNoteStorage";

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
    InitialReviewButtons,
  },
  computed: {
    buttonKey() {
      return this.thing.id;
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
      this.api.reviewMethods
        .doInitialReview({
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
