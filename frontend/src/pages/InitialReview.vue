<template>
  <LoadingPage v-bind="{loading, contentExists: !!reviewPointViewedByUser}">
    <ShowReviewPoint v-bind="reviewPointViewedByUser"/>

    <div>
        <div class="mb-2">
            <input name="note" v-if="reviewPoint.note" :value="reviewPoint.note.id" type="hidden"/>
            <input name="link" v-if="reviewPoint.link" :value="reviewPoint.link.id" type="hidden"/>
            <ReviewSettingForm v-if="!!reviewPointViewedByUser.reviewSetting" v-model="reviewSetting"/>
        </div>
        <input type="submit" name="submit" value="Keep for repetition" class="btn btn-primary"
            v-on:click="processForm(false)"/>
        <input type="submit" name="skip" value="Skip repetition" class="btn btn-secondary"
            v-on:click="processForm(true)">
    </div>
  </LoadingPage>
</template>

<script setup>
import ShowReviewPoint from '../components/review/ShowReviewPoint.vue'
import ReviewSettingForm from '../components/review/ReviewSettingForm.vue'
import LoadingPage from "./LoadingPage.vue"
import { restGet, restPost } from "../restful/restful"
import { ref, computed } from 'vue'

const emit = defineEmit(['redirect'])

const reviewPointViewedByUser = ref(null)
const loading = ref(false)

const reviewPoint = computed(()=>reviewPointViewedByUser.value.reviewPoint)
const reviewSetting = computed(()=>reviewPointViewedByUser.value.reviewSetting)

const loadNew = (resp) => {
  reviewPointViewedByUser.value = resp;
  if (!reviewPointViewedByUser.value.reviewPoint) {
    emit("redirect", {name: "reviews"})
  }
}

const processForm = function(skipReview) {
  if(skipReview) {
    if(!confirm('Are you sure to hide this note from reviewing in the future?')) return;
  }
  reviewPoint.value.removedFromReview = skipReview
  restPost(
      `/api/reviews`,
      {reviewPoint: reviewPoint.value, reviewSetting: reviewSetting.value},
        loading,
        loadNew)
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
