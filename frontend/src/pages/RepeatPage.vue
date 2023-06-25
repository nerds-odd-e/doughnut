<template>
  <div class="header" :class="currentResult ? 'repeat-paused' : ''">
    <RepeatProgressBar
      v-bind="{
        finished,
        toRepeatCount,
        previousResultCursor,
      }"
      @view-last-result="viewLastResult($event)"
    >
    </RepeatProgressBar>
  </div>
  <template v-if="!minimized && toRepeat != undefined">
    <Quiz
      v-if="toRepeatCount !== 0"
      :minimized="minimized"
      :to-repeat="toRepeat"
      :storage-accessor="storageAccessor"
      @answered="onAnswered($event)"
    />
    <template v-else>
      <div class="alert alert-success">
        You have finished all repetitions for this half a day!
      </div>
      <div>
        <button role="button" class="btn btn-secondary" @click="loadMore(3)">
          Load more from next 3 days
        </button>
        <button role="button" class="btn btn-secondary" @click="loadMore(7)">
          Load more from next 7 days
        </button>
        <button role="button" class="btn btn-secondary" @click="loadMore(14)">
          Load more from next 14 days
        </button>
      </div>
    </template>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import _ from "lodash";
import Quiz from "../components/review/Quiz.vue";
import RepeatProgressBar from "../components/review/RepeatProgressBar.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  name: "RepeatPage",
  props: {
    minimized: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    Quiz,
    RepeatProgressBar,
  },
  data() {
    return {
      toRepeat: undefined as number[] | undefined,
      toRepeatCount: 0,
      previousResults: [] as Generated.AnswerResult[],
      previousResultCursor: undefined as number | undefined,
    };
  },
  computed: {
    currentResult() {
      if (this.previousResultCursor === undefined) return undefined;
      return this.previousResults[this.previousResultCursor];
    },
    finished() {
      return this.previousResults.length;
    },
  },
  methods: {
    viewLastResult(cursor: number | undefined) {
      this.previousResultCursor = cursor;
      if (this.currentResult) {
        const { answerId } = this.currentResult;
        this.$router.push({ name: "repeat-answer", params: { answerId } });
        return;
      }
      this.$router.push({ name: "repeat" });
    },

    async loadMore(dueInDays?: number) {
      this.toRepeat = (
        await this.api.reviewMethods.getDueReviewPoints(dueInDays)
      ).toRepeat;
      this.toRepeatCount = this.toRepeat.length;
      if (this.toRepeat?.length === 0) {
        return;
      }
      if (this.api.testability.getEnvironment() !== "testing") {
        this.toRepeat = _.shuffle(this.toRepeat);
      }
    },

    onAnswered(answerResult: Generated.AnswerResult) {
      this.previousResults.push(answerResult);
      this.toRepeatCount -= 1;
      if (!answerResult.correct) {
        this.viewLastResult(this.previousResults.length - 1);
      }
    },
  },
  async mounted() {
    this.loadMore(0);
  },
});
</script>

<style>
.repeat-paused {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
