<template>
  <ContainerPage v-bind="{ contentExists: !!thing }">
    <ProgressBar
      v-bind="{
        paused: minimized,
        title: `Initial Review: `,
        finished,
        toRepeatCount: remainingInitialReviewCountForToday,
      }"
      @resume="resume"
    >
    </ProgressBar>
    <InitialReview
      v-if="!minimized && thing"
      v-bind="{ thing, storageAccessor }"
      @initial-review-done="initialReviewDone"
      @reload-needed="onReloadNeeded"
      :key="thing.note?.id"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { PropType, ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { Thing } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import timezoneParam from "@/managedApi/window/timezoneParam";
import ProgressBar from "@/components/commons/ProgressBar.vue";
import InitialReview from "@/components/review/InitialReview.vue";
import { StorageAccessor } from "@/store/createNoteStorage";
import ContainerPage from "./commons/ContainerPage.vue";

const router = useRouter();
const { managedApi } = useLoadingApi();

defineProps({
  minimized: Boolean,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
defineEmits(["update-reviewing"]);

const finished = ref(0);
const things = ref([] as Thing[]);

const thing = computed(() => things.value[0]);
const remainingInitialReviewCountForToday = computed(() => things.value.length);
const resume = () => {
  router.push({ name: "initial" });
};
const initialReviewDone = () => {
  finished.value += 1;
  things.value.shift();
  if (things.value.length === 0) {
    router.push({ name: "reviews" });
    return;
  }
};

const loadInitialReview = () => {
  managedApi.restReviewsController
    .initialReview(timezoneParam())
    .then((resp) => {
      if (resp.length === 0) {
        router.push({ name: "reviews" });
        return;
      }
      things.value = resp;
    });
};

onMounted(() => {
  loadInitialReview();
});

const onReloadNeeded = () => {
  loadInitialReview();
};
</script>
