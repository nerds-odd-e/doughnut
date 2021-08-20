<template>
    <h1>Joining a Circle</h1>
    <form @submit.prevent.once="processForm">
        <div th:replace="_fragments/forms :: textInput('circle', 'invitationCode', 'Invitation code you got from other people', false)"/>
        <TextInput scopeName='join-circle' field='invitationCode' v-model="formData.invitationCode" :autofocus="true" :errors="formErrors.invitationCode"/>
        <input type="submit" value="Join" class="btn btn-primary"/>
    </form>
</template>

<script>
import TextInput from "../form/TextInput.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  components: { TextInput},
  props: {invitationCode: Number},

  data() {
    return {
      circle: null,
      loading: false,
      formData: { invitationCode: this.invitationCode },
      formErrors: {}
    }
  },

  methods: {
  processForm() {
    restPostMultiplePartForm( `/api/circles/join`, this.formData, r=>this.loading=r)
          .then(res => {
            this.show = false;
            this.$router.push({name: "circleShow", params: {circleId: res.id}})
          })
          .catch(res => this.formErrors = res)
      }
  }
 
}
</script>
