<template>
  <ContainerPage v-bind="{ contentExists: !!thing }">
    <ProgressBar
      v-bind="{
        paused: minimized,
        title: `Initial Review: `,
        finished,
        toRepeatCount: remainingInitialReviewCountForToday,
      }"
    >
      <template #buttons v-if="minimized">
        <div style="display: flex" @click="resume">
          <a title="Go back to review">
            <SvgResume width="30" height="30" />
          </a>
        </div>
      </template>
    </ProgressBar>
    <InitialReview
      v-if="!minimized && thing"
      v-bind="{ thing, storageAccessor }"
      @initial-review-done="initialReviewDone"
      @reload-needed="onReloadNeeded"
      :key="thing.id"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import timezoneParam from "@/managedApi/window/timezoneParam";
import useLoadingApi from "@/managedApi/useLoadingApi";
import ProgressBar from "@/components/commons/ProgressBar.vue";
import SvgResume from "@/components/svgs/SvgResume.vue";
import InitialReview from "@/components/review/InitialReview.vue";
import { StorageAccessor } from "@/store/createNoteStorage";
import ContainerPage from "./commons/ContainerPage.vue";

export default defineComponent({
  name: "InitialReviewPage",
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    minimized: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ContainerPage,
    InitialReview,
    ProgressBar,
    SvgResume,
  },
  emits: ["update-reviewing"],
  data() {
    return {
      finished: 0,
      things: [] as Thing[],
    };
  },
  computed: {
    thing() {
      return this.things[0];
    },
    remainingInitialReviewCountForToday() {
      return this.things.length;
    },
  },
  methods: {
    resume() {
      this.$router.push({ name: "initial" });
    },
    initialReviewDone() {
      this.finished += 1;
      this.things.shift();
      if (this.things.length === 0) {
        this.$router.push({ name: "reviews" });
        return;
      }
    },
    onReloadNeeded() {
      this.loadInitialReview();
    },
    loadInitialReview() {
      this.managedApi.restReviewsController
        .initialReview(timezoneParam())
        .then((resp) => {
          if (resp.length === 0) {
            this.$router.push({ name: "reviews" });
            return;
          }
          this.things = resp;
        });
    },
  },
  mounted() {
    this.loadInitialReview();
  },
});
</script>
