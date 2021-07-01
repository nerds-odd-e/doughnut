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

<script>
import ShowReviewPoint from '../components/review/ShowReviewPoint.vue'
import ReviewSettingForm from '../components/review/ReviewSettingForm.vue'
import LoadingPage from "./commons/LoadingPage.vue"
import { restGet, restPost } from "../restful/restful"

export default {
  name: 'InitialReview',
  components: {ShowReviewPoint, ReviewSettingForm, LoadingPage},
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

    submitInitialReview() {
      restPost(
        `/api/reviews/${this.repetition.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`,
        data,
        (val)=>this.loading=val,
        this.loadNew)
    }
  },
  mounted(){
    this.fetchData();
  }
}

</script>
