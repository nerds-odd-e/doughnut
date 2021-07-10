<template>
  <h3>Edit review setting for <em>{{title}}</em></h3>
  <form @submit.prevent="processForm">
      <ReviewSettingForm scopeName='review-setting' v-model="formData" :errors="formErrors"/>
      <input type="submit" value="Update" class="btn btn-primary"/>
  </form>
</template>

<script>
import ReviewSettingForm from "./ReviewSettingForm.vue"
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
import { restGet, restPost } from "../../restful/restful"

export default {
  components: { ReviewSettingForm, SvgReviewSetting },
  props: {noteId: Number, title: String},
  emits: ['done'],
  data() {
    return {
      formData: {},
      formErrors: {},
      loading: false
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteId}/review-setting`,
        (r)=>this.loading=r,
        (res) => this.formData = res)
    },
    processForm() {
      restPost(
        `/api/notes/${this.noteId}/review-setting`,
        this.formData,
        r=>this.loading=r,
        (res) => {
          this.$emit('done')
          this.show = false
        },
        (res) => this.formErrors = res
      )
    }

  },
  mounted() {
    this.fetchData()
  }
}
</script>
