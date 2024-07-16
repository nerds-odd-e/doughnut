<template>
  <h3>Edit notebook settings</h3>
  <form  @submit="processForm">
    <div class="form-container">
      <CheckInput
        scope-name="notebook"
        field="skipReviewEntirely"
        v-model="formData.skipReviewEntirely"
        :errors="errors.skipReviewEntirely"
      />
      <TextInput
        scope-name="notebook"
        field="numberOfQuestionsInAssessment"
        v-model="formData.numberOfQuestionsInAssessment"
        :errors="errors.numberOfQuestionsInAssessment"
      />
    <div>
      <label for="untilCertExpire">Until Cert Expire (in days)</label>
      <input type="number" id="untilCertExpire" v-model.number="formData.untilCertExpire" class="form-control" />
    </div>
    <input class="btn btn-primary btn-layout" type="submit" value="Update"  /></div>
  </form>
</template>

<script>
import CheckInput from "@/components/form/CheckInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "../form/TextInput.vue"

export default {
  setup() {
    return useLoadingApi()
  },
  props: { notebook: Object },
  components: { CheckInput, TextInput },
  data() {
    const {
      skipReviewEntirely,
      numberOfQuestionsInAssessment,
      untilCertExpire,
    } = this.notebook.notebookSettings
    return {
      formData: {
        skipReviewEntirely,
        numberOfQuestionsInAssessment,
        untilCertExpire,
      },
      errors: {},
    }
  },
  methods: {
    processForm() {
      this.managedApi.restNotebookController
        .update1(this.notebook.id, this.formData)
        .then(() => {
          this.$router.push({ name: "notebooks" })
        })
        .catch((err) => (this.errors = err))
    },
  },
}
</script>
<style>
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.form-container {
  display: flex;
  flex-direction: column;
  row-gap: 12px;

}

.btn-layout {
  width: 100px;
  align-self: center;
}

</style>