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
import { restGet, restPost } from "../../restful/restful";

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
      restGet(`/api/notes/${this.noteId}/review-setting`)
      .then((res) => {
          this.formData = res
        }
      )
      .finally(() => this.loading = false)
    },
    processForm() {
      restPost(
        `/api/notes/${this.noteId}/review-setting`,
        this.formData,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$emit("done");
        })
        .catch((res) => (this.formErrors = res));
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
