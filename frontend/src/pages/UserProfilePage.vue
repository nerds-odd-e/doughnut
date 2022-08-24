<template>
  <ContainerPage
    v-bind="{ loading, contentExists: !!formData, title: 'Edit User Setting' }"
  >
    <div v-if="!!formData">
      <form @submit.prevent.once="processForm">
        <TextInput
          scope-name="user"
          field="name"
          v-model="formData.name"
          :autofocus="true"
          :errors="formErrors.name"
        />
        <TextInput
          scope-name="user"
          field="dailyNewNotesCount"
          v-model="formData.dailyNewNotesCount"
          :errors="formErrors.dailyNewNotesCount"
        />
        <TextInput
          scope-name="user"
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
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default {
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  components: { ContainerPage, TextInput },
  emits: ["userUpdated"],
  data() {
    return {
      formData: null,
    };
  },
  methods: {
    fetchData() {
      this.api.userMethods.currentUser().then((res) => {
        this.formData = res;
      });
    },
    processForm() {
      this.storedApi
        .updateUser(this.formData.id, this.formData)
        .then((user) => {
          this.piniaStore.setCurrentUser(user);
          this.$router.push({ name: "root" });
        });
    },
  },

  mounted() {
    this.fetchData();
  },
};
</script>
