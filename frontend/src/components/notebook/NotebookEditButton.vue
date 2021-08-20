<template>
  <ModalWithButton v-model="show">
      <template #button>
        <button class="btn btn-sm" role="button" @click="show=true" title="Edit notebook settings">
            <SvgEditNotebook/>
        </button>
      </template>
      <template #header>
        <h3>Edit notebook settings</h3>
      </template>
      <template #body>
        <form @submit.prevent.once="processForm">
            <CheckInput scopeName='notebook' field='skipReviewEntirely' v-model="formData.skipReviewEntirely" :errors="formErrors.skipReviewEntirely"/>
            <input type="submit" value="Update" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>


</template>

<script>
import SvgEditNotebook from "../svgs/SvgEditNotebook.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import CheckInput from "../form/CheckInput.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  props: {notebook: Object},
  components: { CheckInput, ModalWithButton, SvgEditNotebook },
  data(){
    const { skipReviewEntirely } = this.notebook
    return {
      show: false, 
      formData: { skipReviewEntirely },
      formErrors: {}
    }
  },

  methods: {
  processForm() {
    restPostMultiplePartForm( `/api/notebooks/${this.notebook.id}`, this.formData, r=>this.loading=r)
          .then(res => {
            this.show = false;
            this.$router.push({name: "notebooks"})
          })
          .catch(res => this.formErrors = res)
      }
  }
 
}
</script>
