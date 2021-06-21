<template>
  <LoadingPage v-bind="{loading, contentExists: !!noteViewedByUser}">
  <div th:replace="_fragments/review_fragments :: showReviewPoint(${reviewPoint})"/>

  <form id="review-setting" ction="#" th:action="@{/reviews/}" th:object="${reviewPoint}" method="post">
      <div class="mb-2">
          <input th:field="*{note}" type="hidden"/>
          <input th:field="*{link}" type="hidden"/>
          <div th:if="${reviewSetting}">
              <div th:replace="_fragments/note_fragments :: reviewSettingForm(${reviewSetting})"/>
          </div>
      </div>
      <input type="submit" name="submit" value="Keep for repetition" class="btn btn-primary"/>
      <input type="submit" name="skip" value="Skip repetition" class="btn btn-secondary"
          onclick="return confirm('Are you sure to hide this note from reviewing in the future?')">
  </form>
  </LoadingPage>
</template>

<script>
import Quiz from '../components/review/Quiz.vue'
import Repetition from '../components/review/Repetition.vue'
import LoadingPage from "./LoadingPage.vue"
import { restGet, restPost } from "../restful/restful"
import { ref, inject } from 'vue'

export default {
  components: {
    Quiz, Repetition, ContentLoader, LoadingThinBar
  },
  data() {
    return {
      repetition: ref(null),
      answerResult: ref(null),
      loading: ref(false)
    }
  },

  mounted() {
    this.fetchData();
  },

  methods: {
    fetchData() {
      restGet(`/api/reviews/repeat`, (val)=>this.loading=val, this.loadNew)
    },

    processAnswer(answerData) {
      restPost(
        `/api/reviews/${this.repetition.reviewPointViewedByUser.reviewPoint.id}/answer`,
        answerData,
         (val)=>this.loading=val,
         (res)=>this.answerResult = res)
    },

    selfEvaluate(data) {
      restPost(
        `/api/reviews/${this.repetition.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`,
        data,
         (val)=>this.loading=val,
         this.loadNew)
    },

    loadNew(resp) {
      this.repetition = resp;
      this.answerResult = null;
      if (!this.repetition.reviewPointViewedByUser) {
        this.$router.push({name: "reviews"})
      }
    }

  }

}

</script>
