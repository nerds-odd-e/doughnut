<template>
  <div class="container">
    <ContentLoader v-if="!answeredQuestion" />
    <AnsweredQuestionComponent
      v-else
      v-bind="{ answeredQuestion, storageAccessor }"
    />
  </div>
</template>

<script lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"
import { AnsweredQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, defineComponent } from "vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: {
    answerId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ContentLoader,
    AnsweredQuestionComponent,
  },
  data() {
    return {
      answeredQuestion: undefined as AnsweredQuestion | undefined,
    }
  },
  computed: {
    reviewPoint() {
      return this.answeredQuestion?.reviewPoint
    },
  },
  methods: {
    async fetchData() {
      this.answeredQuestion =
        await this.managedApi.restReviewsController.showAnswer(this.answerId)
    },
  },
  watch: {
    answerId() {
      this.fetchData()
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>
