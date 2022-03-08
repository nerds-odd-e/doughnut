<template>
  <h3>
    Edit review settings for <em>{{ title }}</em>
  </h3>
  <form @submit.prevent="processForm">
    <ReviewSettingForm
      :showLevel="true"
      scopeName="review-setting"
      v-model="formData"
      :errors="formErrors"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script>
import ReviewSettingForm from "./ReviewSettingForm.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";
import useLoadingApi from '../../managedApi/useLoadingApi';

export default {
  setup() {
    return useLoadingApi({initalLoading: true, hasFormError: true});
  },
  components: { ReviewSettingForm, SvgReviewSetting },
  props: { noteId: [String, Number], title: String },
  emits: ["done"],
  data() {
    return {
      formData: {},
    };
  },
  methods: {
    fetchData() {
      this.api.reviewMethods.getReviewSetting(this.noteId)
      .then((res) => { this.formData = res })
    },
    processForm() {
      this.api.reviewMethods.updateReviewSetting(this.noteId, this.formData)
        .then((res) => this.$emit("done"))
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
