<template>
  <div class="container">
    <LoadingPage v-bind="{ contentExists: !!answeredQuestion }">
      <AnsweredQuestionComponent
        v-if="answeredQuestion"
        v-bind="{ answeredQuestion, storageAccessor }"
        @self-evaluated="onSelfEvaluated($event)"
      />
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { AnsweredQuestion, ReviewPoint } from "@/generated/backend";
import LoadingPage from "./commons/LoadingPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import AnsweredQuestionComponent from "../components/review/AnsweredQuestionComponent.vue";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    answerId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    LoadingPage,
    AnsweredQuestionComponent,
  },
  data() {
    return {
      answeredQuestion: undefined as AnsweredQuestion | undefined,
    };
  },
  computed: {
    reviewPoint() {
      return this.answeredQuestion?.reviewPoint;
    },
  },
  methods: {
    onSelfEvaluated(reviewPoint: ReviewPoint) {
      if (!this.answeredQuestion) return;
      this.answeredQuestion = {
        ...this.answeredQuestion,
        reviewPoint,
      };
    },
    async fetchData() {
      this.answeredQuestion =
        await this.managedApi.restReviewsController.showAnswer(this.answerId);
    },
  },
  watch: {
    answerId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
