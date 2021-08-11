<template>
  <LoadingPage v-bind="{loading, contentExists: !!reviewPointViewedByUser}">
    <template v-if="!!reviewPoint">
      <Minimizable :minimized="nested">
        <template #minimizedContent>
          <div class="initial-review-container" v-on:click="$router.push({name: 'initial'})">
            <InitialReviewButtons :key="buttonKey" @doInitialReview="processForm($event)"/>
          </div>
        </template>
        <template #fullContent>
              <ShowReviewPoint v-bind="reviewPointViewedByUser" @updated="fetchData()"/>
              <div>
                <div class="mb-2">
                    <ReviewSettingForm v-if="!!reviewPointViewedByUser.reviewSetting" v-model="reviewSetting" :errors="{}"/>
                </div>
                <InitialReviewButtons :key="buttonKey" @doInitialReview="processForm($event)"/>
              </div>
        </template>
      </Minimizable>
    </template>
  </LoadingPage>
</template>

<script>
import ShowReviewPoint from '../components/review/ShowReviewPoint.vue'
import ReviewSettingForm from '../components/review/ReviewSettingForm.vue'
import InitialReviewButtons from '../components/review/InitialReviewButtons.vue'
import LoadingPage from "./commons/LoadingPage.vue"
  import Minimizable from "../components/commons/Minimizable.vue"
import { restGet, restPost } from "../restful/restful"

export default {
  name: 'InitialReviewPage',
  props: { nested: Boolean },
  components: {ShowReviewPoint, ReviewSettingForm, LoadingPage, InitialReviewButtons, Minimizable},
  data() {
    return {
      reviewPointViewedByUser: null,
      loading: null
    }
  },
  computed: {
    reviewPoint() { return this.reviewPointViewedByUser.reviewPoint },
    reviewSetting() { return this.reviewPointViewedByUser.reviewSetting },
    buttonKey() {return !!this.reviewPoint.noteId ? `note-${this.reviewPoint.noteId}` : `link-${this.reviewPoint.linkId}`}
  },

  methods: {
    loadNew(resp){
      this.reviewPointViewedByUser = resp;
      if (!this.reviewPointViewedByUser.reviewPoint) {
        this.$router.push({name: "reviews"})
      }
    },

    async processForm(skipReview) {
      if(skipReview) {
        if(!await this.$popups.confirm('Are you sure to hide this note from reviewing in the future?')) return;
      }
      this.reviewPoint.removedFromReview = skipReview
      restPost(
          `/api/reviews`,
          {reviewPoint: this.reviewPoint, reviewSetting: this.reviewSetting},
          r=>this.loading=r)
        .then(this.loadNew)
    },

    fetchData() {
      restGet(`/api/reviews/initial`, r=>this.loading=r).then(this.loadNew)
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
