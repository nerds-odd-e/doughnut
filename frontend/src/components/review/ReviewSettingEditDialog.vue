<template>
  <h3>
    Edit review settings for <em>{{ title }}</em>
  </h3>
  <form @submit.prevent="processForm">
    <ReviewSettingForm
      :show-level="true"
      scope-name="review-setting"
      v-model="formData"
      :errors="formErrors"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ReviewSettingForm from "./ReviewSettingForm.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: true });
  },
  components: { ReviewSettingForm, SvgReviewSetting },
  props: { noteId: { type: Number, required: true }, title: String },
  emits: ["done"],
  data() {
    return {
      formData: {} as Omit<Generated.ReviewSetting, "id">,
    };
  },
  methods: {
    fetchData() {
      this.api.reviewMethods.getReviewSetting(this.noteId).then((res) => {
        this.formData = res;
      });
    },
    processForm() {
      this.api.reviewMethods
        .updateReviewSetting(this.noteId, this.formData)
        .then(() => this.$emit("done"));
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
