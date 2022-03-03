<template>
  <h3>
    Edit review setting for <em>{{ title }}</em>
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
import api from  "../../managedApi/api";

export default {
  components: { ReviewSettingForm, SvgReviewSetting },
  props: { noteId: [String, Number], title: String },
  emits: ["done"],
  data() {
    return {
      formData: {},
      formErrors: {},
      loading: true,
    };
  },
  methods: {
    fetchData() {
      this.loading = true
      api(this).reviewMethods.getReviewSetting(this.noteId)
      .then((res) => { this.formData = res })
      .finally(() => this.loading = false)
    },
    processForm() {
      this.loading = true
      api(this).reviewMethods.updateReviewSetting(this.noteId, this.formData)
        .then((res) => {
          this.$emit("done");
        })
        .catch((res) => (this.formErrors = res))
        .finally(() => this.loading = false)
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
