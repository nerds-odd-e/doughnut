<template>
  <LoadingPage v-bind="{ loading, contentExists: answerResult }">
    <AnswerResult v-if="answerResult" v-bind="{answerResult}"/>
    <ShowReviewPoint
      v-bind="{ reviewPointViewedByUser }"
    />

  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import LoadingPage from "./commons/LoadingPage.vue";
import NoteSphereComponent from '../components/notes/views/NoteSphereComponent.vue';
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import AnswerResult from "../components/review/AnswerResult.vue";
import ShowReviewPoint from '../components/review/ShowReviewPoint.vue';

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true});
  },
  name: "NoteShowPage",
  props: { answerId: Number },
  components: { LoadingPage, NoteSphereComponent, AnswerResult, ShowReviewPoint },
  data() {
    return {
      answerResult: undefined as Generated.AnswerResult | undefined
    }
  },
  computed: {
    reviewPointViewedByUser() {
      return this.answerResult?.reviewPoint;
    }
  },
  methods: {
    async fetchData() {
      this.answerResult = await this.storedApi.reviewMethods.getAnswer(this.answerId);
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