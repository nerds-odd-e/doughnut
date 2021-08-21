<template>
<div class="container">

  <h2>Welcome, new user. Please create your profile</h2>
  <LoadingPage v-bind="{loading, contentExists: true}">
    <form @submit.prevent.once="processForm">
      <TextInput scopeName='user' field='name' v-model="formData.name" :autofocus="true" :errors="formErrors.name" :placeholder="Nickname"/>
      <input type="submit" value="Submit" class="btn btn-primary"/>
    </form>
  </LoadingPage>
</div>
</template>

<script setup>
import LoadingPage from "./commons/LoadingPage.vue"
import TextInput from "../components/form/TextInput.vue"
import {restPostMultiplePartForm} from "../restful/restful"
import { ref } from "@vue/reactivity"

const emits = defineEmits(['userCreated'])
const loading = ref(false)
const formData = ref ({})
const formErrors = ref({})
const processForm = () => {
      restPostMultiplePartForm(`/api/user`, formData.value, loading)
        .then(res => emits('userCreated', res))
        .catch(res => formErrors.value = res)
    }
</script>
