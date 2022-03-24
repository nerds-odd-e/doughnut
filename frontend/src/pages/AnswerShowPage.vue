<template>
  <LoadingPage v-bind="{ loading, contentExists: answerResult }">
    <AnswerResult v-if="answerResult" v-bind="{answerResult}"/>
    <Repetition
      v-bind="{ reviewPointViewedByUser, answerResult }"
    />

  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import LoadingPage from "./commons/LoadingPage.vue";
import NoteSphereComponent from '../components/notes/views/NoteSphereComponent.vue';
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import AnswerResult from "../components/review/AnswerResult.vue";
import Repetition from '../components/review/Repetition.vue';
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue';

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true});
  },
  name: "NoteShowPage",
  props: { answerId: Number },
  components: { LoadingPage, NoteSphereComponent, AnswerResult, Repetition, NoteStatisticsButton },
  data() {
    return {
      answerResult: undefined as Generated.AnswerViewedByUser | undefined
    }
  },
  computed: {
    reviewPointViewedByUser() {
      return this.answerResult?.reviewPoint;
    },
    reviewPoint() {
      return this.reviewPointViewedByUser?.reviewPoint;
    },
    noteId() {
      return this.reviewPoint?.noteId
    },
    linkId() {
      return this.reviewPoint?.linkId
    },
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