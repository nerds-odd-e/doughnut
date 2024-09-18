<template>
  <div v-if="!minimized" class="content">
    <ContentLoader v-if="!currentQuestionFetched" />
    <template v-else>
      <div v-if="!currentReviewQuestion">
        <JustReview
          v-bind="{
            reviewPointId: currentReviewPointId,
            storageAccessor,
          }"
          @reviewed="onAnswered($event)"
        />
      </div>
      <ContestableQuestion
        v-else
        v-bind="{
          reviewQuestionInstance: currentReviewQuestion,
          storageAccessor,
        }"
        @answered="onAnswered($event)"
        :key="currentReviewQuestion.id"
      />
    </template>
  </div>
</template>

<script lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { AnsweredQuestion, ReviewQuestionInstance } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import _ from "lodash"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: {
    minimized: Boolean,
    reviewPoints: {
      type: Object as PropType<number[]>,
      required: true,
    },
    currentIndex: {
      type: Number,
      required: true,
    },
    eagerFetchCount: {
      type: Number,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["answered"],
  components: {
    JustReview,
    ContentLoader,
    ContestableQuestion,
  },
  data() {
    return {
      reviewQuestionCache: [] as (ReviewQuestionInstance | undefined)[],
      eagerFetchUntil: 0,
      fetching: false,
    }
  },
  computed: {
    currentReviewPointId() {
      return this.reviewPointIdAt(this.currentIndex)
    },
    currentQuestionFetched() {
      return this.reviewQuestionCache.length > this.currentIndex
    },
    currentReviewQuestion() {
      return this.reviewQuestionCache[this.currentIndex]
    },
  },
  watch: {
    minimized() {
      this.selectPosition()
    },
    currentIndex() {
      this.fetchQuestion()
    },
    currentReviewQuestion() {
      this.selectPosition()
    },
  },
  methods: {
    reviewPointIdAt(index: number): number | undefined {
      if (this.reviewPoints && index < this.reviewPoints.length) {
        return this.reviewPoints[index] as number
      }
      return undefined
    },

    selectPosition() {
      if (this.minimized) return
    },

    async fetchQuestion() {
      this.eagerFetchUntil = _.max([
        this.eagerFetchUntil,
        this.currentIndex + this.eagerFetchCount,
      ]) as number
      if (!this.fetching) {
        this.fetching = true
        await this.fetchNextQuestion()
        this.fetching = false
      }
    },

    async fetchNextQuestion() {
      const index = this.reviewQuestionCache.length
      if (this.eagerFetchUntil <= index) return
      const reviewPointId = this.reviewPointIdAt(index)
      if (reviewPointId === undefined) return
      try {
        const question =
          await this.managedApi.silent.restReviewQuestionController.generateRandomQuestion(
            reviewPointId
          )
        this.reviewQuestionCache.push(question)
      } catch (e) {
        this.reviewQuestionCache.push(undefined)
      }
      await this.fetchNextQuestion()
    },

    onAnswered(answerResult: AnsweredQuestion) {
      this.$emit("answered", answerResult)
    },
  },
  async mounted() {
    this.fetchQuestion()
  },
})
</script>
