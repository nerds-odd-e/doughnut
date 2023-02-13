<template>
  <ContainerPage v-bind="{ contentExists: true }">
    <ShowReviewPoint
      v-bind="{ reviewPoint, expandInfo: true, storageAccessor }"
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
import ShowReviewPoint from "./ShowReviewPoint.vue";
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
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["reloadNeeded", "initialReviewDone"],
  components: {
    ShowReviewPoint,
    ContainerPage,
    InitialReviewButtons,
  },
  computed: {
    buttonKey() {
      return this.reviewPoint?.thing?.id;
    },
  },

  methods: {
    async processForm(skipReview: boolean) {
      if (skipReview) {
        if (
          !(await this.popups.confirm(
            "Confirm to hide this note from reviewing in the future?"
          ))
        )
          return;
      }
      this.api.reviewMethods
        .doInitialReview({
          thingId: this.reviewPoint.thing.id,
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
