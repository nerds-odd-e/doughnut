<template>
  <StickTopBar v-if="nested">
    <div class="initial-review-container" v-on:click="$router.push({name: 'initial'})">
      <InitialReviewButtons @doInitialReview="processForm($event)"/>
    </div>
  </StickTopBar>
  <div v-else>
    <LoadingPage v-bind="{loading, contentExists: !!reviewPointViewedByUser}">
      <ShowReviewPoint v-bind="reviewPointViewedByUser"/>

      <div>
          <div class="mb-2">
              <input name="note" v-if="reviewPoint.note" :value="reviewPoint.note.id" type="hidden"/>
              <input name="link" v-if="reviewPoint.link" :value="reviewPoint.link.id" type="hidden"/>
              <ReviewSettingForm v-if="!!reviewPointViewedByUser.reviewSetting" v-model="reviewSetting"/>
          </div>
          <InitialReviewButtons @doInitialReview="processForm($event)"/>
      </div>
    </LoadingPage>
  </div>
</template>

<script>
import ShowReviewPoint from '../components/review/ShowReviewPoint.vue'
import ReviewSettingForm from '../components/review/ReviewSettingForm.vue'
import InitialReviewButtons from '../components/review/InitialReviewButtons.vue'
import LoadingPage from "./commons/LoadingPage.vue"
  import StickTopBar from "../components/StickTopBar.vue"
import { restGet, restPost } from "../restful/restful"

export default {
  name: 'InitialReviewPage',
  props: { nested: Boolean },
  components: {ShowReviewPoint, ReviewSettingForm, LoadingPage, InitialReviewButtons, StickTopBar},
  data() {
    return {
      reviewPointViewedByUser: null,
      loading: null
    }
  },
  computed: {
    reviewPoint() { return this.reviewPointViewedByUser.reviewPoint },
    reviewSetting() { return this.reviewPointViewedByUser.reviewSetting },
  },

  methods: {
    loadNew(resp){
      this.reviewPointViewedByUser = resp;
      if (!this.reviewPointViewedByUser.reviewPoint) {
        this.$router.push({name: "reviews"})
      }
    },

    processForm(skipReview) {
      if(skipReview) {
        if(!confirm('Are you sure to hide this note from reviewing in the future?')) return;
      }
      this.reviewPoint.removedFromReview = skipReview
      restPost(
          `/api/reviews`,
          {reviewPoint: this.reviewPoint, reviewSetting: this.reviewSetting},
          r=>this.loading=r,
          this.loadNew)
    },

    fetchData() {
      restGet(`/api/reviews/initial`, r=>this.loading=r, this.loadNew)
    },
  },
  mounted(){
    this.fetchData();
  }
}
</script>

<style>
.initial-review-container {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
