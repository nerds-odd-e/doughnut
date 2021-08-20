<template>
  <ModalWithButton v-model="show">
      <template #button>
        <button class="btn btn-sm" role="button" @click="show=true" title="Edit subscription">
            <SvgEdit/>
        </button>
      </template>
      <template #header>
        <h3>Edit subscription</h3>
      </template>
      <template #body>
        <form @submit.prevent.once="processForm">
            <TextInput scopeName='subscription' field='dailyTargetOfNewNotes' v-model="formData.dailyTargetOfNewNotes" :errors="formErrors.dailyTargetOfNewNotes"/>
            <input type="submit" value="Update" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>


</template>

<script>
import SvgEdit from "../svgs/SvgEdit.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import TextInput from "../form/TextInput.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  props: {subscription: Object},
  components: { SvgEdit, TextInput, ModalWithButton },
  data(){
    const { dailyTargetOfNewNotes } = this.subscription
    return {
      show: false, 
      formData: { dailyTargetOfNewNotes },
      formErrors: {}
    }
  },

  methods: {
  processForm() {
    restPostMultiplePartForm( `/api/subscriptions/${this.subscription.id}`, this.formData, r=>this.loading=r)
          .then(res => {
            this.show = false;
            this.$router.push({name: "notebooks"})
          })
          .catch(res => this.formErrors = res)
      }
  }
 
}
</script>
