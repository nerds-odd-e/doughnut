<template>
  <BreadcrumbWithCircle v-bind="{ ancestors: [], fromBazaar: true }" />
  <ContainerPage
    v-bind="{
      loading,
      contentExists: !!notebooksViewedByUser,
      title: 'Welcome To The Bazaar',
    }"
  >
    <p>These are shared notes from doughnut users.</p>
    <div v-if="!!notebooksViewedByUser">
      <NotebookBazaarViewCards
        :notebooks="notebooksViewedByUser.notebooks"
        :logged-in="!!user"
      />
    </div>
  </ContainerPage>
</template>

<script>
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue";
import NotebookBazaarViewCards from "../components/bazaar/NotebookBazaarViewCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default {
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  name: "NotebooksPage",
  components: { ContainerPage, NotebookBazaarViewCards, BreadcrumbWithCircle },
  data() {
    return {
      notebooksViewedByUser: null,
    };
  },
  computed: {
    user() {
      return this.piniaStore.currentUser;
    },
  },

  methods: {
    fetchData() {
      this.api.getBazaar().then((res) => {
        this.notebooksViewedByUser = res;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
