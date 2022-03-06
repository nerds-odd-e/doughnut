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
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  name: "NotebooksPage",
  components: { ContainerPage, NotebookBazaarViewCards },
  data() {
    return {
      loading: true,
      notebooksViewedByUser: null,
    };
  },
  computed: {
    user() { return this.store.getCurrentUser()}
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
