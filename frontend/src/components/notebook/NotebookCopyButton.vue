<template>
  <ModalWithButton v-model="show">
    <template #button>
      <button
          class="btn btn-sm"
          role="button"
          @click="show = true"
          title="Copy notebook"
      >
        <SvgFailed/>
      </button>
    </template>
    <template #header>
      <h3>Copy notebook</h3>
    </template>
    <template #body>
      <form @submit.prevent.once="processForm">
        <input type="submit" value="Copy" class="btn btn-primary"/>
      </form>
    </template>
  </ModalWithButton>
</template>

<script>
import SvgFailed from "../svgs/SvgFailed.vue";
import ModalWithButton from "../commons/ModalWithButton.vue";
import {restPost} from "../../restful/restful";

export default {
  props: {notebook: Object},
  components: {ModalWithButton, SvgFailed},
  data() {
    const {skipReviewEntirely} = this.notebook;
    return {
      show: false,
      formData: {skipReviewEntirely},
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      restPost(
          `/api/notebooks/${this.notebook.id}/copy`,
          {},
          (r) => {
          }
      ).then((res) => {
        this.show = false;
        this.$router.push({name: "notebooks"});
      }).catch((res) => (this.formErrors = res));
    },
  },
};
</script>
