<template>
  <ContainerPage v-bind="{ loading, contentExists: !!formData, title: 'Edit User Setting' }">
    <div v-if="!!formData">
      <form @submit.prevent.once="processForm">
        <TextInput
          scopeName="user"
          field="name"
          v-model="formData.name"
          :autofocus="true"
          :errors="formErrors.name"
        />
        <TextInput
          scopeName="user"
          field="dailyNewNotesCount"
          v-model="formData.dailyNewNotesCount"
          :errors="formErrors.dailyNewNotesCount"
        />
        <TextInput
          scopeName="user"
          field="spaceIntervals"
          v-model="formData.spaceIntervals"
          :errors="formErrors.spaceIntervals"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </div>
  </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import TextInput from "../components/form/TextInput.vue";
import api from "../managedApi/api"
import storedApi from "../managedApi/storedApi"

export default {
  components: { ContainerPage, TextInput },
  emits: ["userUpdated"],
  data() {
    return {
      loading: true,
      formData: null,
      formErrors: {},
    };
  },
  methods: {
    fetchData() {
      api(this).userMethods.currentUser().then((res) => {
        this.formData = res
      })
      .finally(() => this.loading = false);
    },
    processForm() {
      this.loading = true
      storedApi(this.$store).updateUser(this.formData.id, this.formData)
        .then(() =>  this.$router.push({ name: "root" }))
        .catch((res) => (this.formErrors = res))
        .finally(()=> this.loading = false)

    },
  },

  mounted() {
    this.fetchData();
  },
};
</script>
