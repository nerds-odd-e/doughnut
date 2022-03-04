<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notebooksViewedByUser, title: 'Welcome To The Bazaar' }">
    <p>These are shared notes from doughnut users.</p>
    <div v-if="!!notebooksViewedByUser">
      <NotebookBazaarViewCards
        :notebooks="notebooksViewedByUser.notebooks"
        :user="user"
      />
    </div>
  </ContainerPage>
</template>

<script>
import NotebookBazaarViewCards from "../components/bazaar/NotebookBazaarViewCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import api from  "../managedApi/api";

export default {
  name: "NotebooksPage",
  components: { ContainerPage, NotebookBazaarViewCards },
  data() {
    return {
      loading: true,
      notebooksViewedByUser: null,
    };
  },
  computed: {
    user() { return this.$store.getters.getCurrentUser()}
  },

  methods: {
    fetchData() {
      api(this).getBazaar().then(
        (res) => {
          this.notebooksViewedByUser = res
        }
      )
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
