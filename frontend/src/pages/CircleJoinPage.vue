<template>
  <LoadingPage v-bind="{loading, contentExists: true}">
    <h1>Joining a Circle</h1>
    <form :action="`/circles/join`" method="post">
        <div th:replace="_fragments/forms :: textInput('circle', 'invitationCode', 'Invitation code you got from other people', false)"/>
        <TextInput scopeName='join-circle' field='invitationCode' v-model="invitationCode" :autofocus="true"/>
        <input type="submit" value="Submit" class="btn btn-primary"/>
    </form>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue"
import TextInput from "../components/form/TextInput.vue"
import { restPost } from "../restful/restful"

export default {
  components: { TextInput, LoadingPage},
  props: {invitationCode: Number},

  data() {
    return {
      circle: null,
      loading: false,
      formErrors: {}
    }
  },

  methods: {
  },

}
</script>
