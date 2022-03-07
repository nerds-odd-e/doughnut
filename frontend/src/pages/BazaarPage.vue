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
import storedComponent from "../store/storedComponent";
import useLoadingApi from "../managedApi/useLoadingApi";

export default storedComponent({
  setup() {
    return useLoadingApi({initalLoading: true});
  },
  name: "NotebooksPage",
  components: { ContainerPage, NotebookBazaarViewCards },
  data() {
    return {
      notebooksViewedByUser: null,
    };
  },
  computed: {
    user() { return this.piniaStore.currentUser }
  },

  methods: {
    fetchData() {
      this.apiExp().getBazaar().then(
        (res) => {
          this.notebooksViewedByUser = res
        }
      )
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
