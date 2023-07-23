<template>
  <div class="container">
    <LoadingPage v-bind="{ contentExists: !!answeredQuestion }">
      <AnsweredQuestion
        v-if="answeredQuestion"
        v-bind="{ answeredQuestion, storageAccessor }"
        @self-evaluated="onSelfEvaluated($event)"
      />
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingPage from "./commons/LoadingPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import AnsweredQuestion from "../components/review/AnsweredQuestion.vue";
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
    AnsweredQuestion,
  },
  data() {
    return {
      answeredQuestion: undefined as Generated.AnsweredQuestion | undefined,
    };
  },
  computed: {
    reviewPoint() {
      return this.answeredQuestion?.reviewPoint;
    },
  },
  methods: {
    onSelfEvaluated(reviewPoint: Generated.ReviewPoint) {
      if (!this.answeredQuestion) return;
      this.answeredQuestion = {
        ...this.answeredQuestion,
        reviewPoint,
      };
    },
    async fetchData() {
      this.answeredQuestion = await this.api.reviewMethods.getAnswer(
        this.answerId,
      );
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
