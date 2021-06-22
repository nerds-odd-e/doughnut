<template>
  <LoadingPage v-bind="{loading, contentExists: !!reviewPointViewedByUser}">
    <ShowReviewPoint v-bind="reviewPointViewedByUser"/>

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

<script setup>
import ShowReviewPoint from '../components/review/ShowReviewPoint.vue'
import LoadingPage from "./LoadingPage.vue"
import { restGet, restPost } from "../restful/restful"
import { ref } from 'vue'

const emit = defineEmit(['redirect'])

const reviewPointViewedByUser = ref(null)
const loading = ref(false)

const loadNew = (resp) => {
  reviewPointViewedByUser.value = resp;
  if (!eviewPointViewedByUser.value.reviewPoint) {
    emit("redirect", {name: "reviews"})
  }
}


const fetchData = () => {
      restGet(`/api/reviews/initial`, loading, loadNew)
    }

const submitInitialReview = () => {
      restPost(
        `/api/reviews/${this.repetition.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`,
        data,
         (val)=>this.loading=val,
         this.loadNew)
    }

fetchData();

</script>
