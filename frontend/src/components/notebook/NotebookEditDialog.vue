<template>
  <h3>Edit notebook settings</h3>
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
    <TextInput
      scope-name="notebook"
      field="untilCertExpire"
      v-model="formData.untilCertExpire"
      :errors="errors.untilCertExpire"
    />
    <button class="btn btn-primary btn-layout" @click="processForm">
      Update
    </button>
    </div>
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
          this.$router.go()
        })
        .catch((err) => (this.errors = err))
    },
  },
}
</script>
