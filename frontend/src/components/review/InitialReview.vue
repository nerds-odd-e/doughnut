<template>
  <ContainerPage v-bind="{ loading, contentExists: true }">
    <template v-if="!nested">
      <ShowReviewPoint
        v-bind="{ reviewPoint, expandInfo: true }"
        @level-changed="$emit('reloadNeeded', $event)"
        @note-deleted="$emit('reloadNeeded', $event)"
        @link-deleted="$emit('reloadNeeded', $event)"
      />
      <InitialReviewButtons
        :key="buttonKey"
        @do-initial-review="processForm($event)"
      />
    </template>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import InitialReviewButtons from "./InitialReviewButtons.vue";
import ContainerPage from "../../pages/commons/ContainerPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  name: "InitialReview",
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    nested: Boolean,
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
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
          this.$emit("initialReviewDone", data);
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
