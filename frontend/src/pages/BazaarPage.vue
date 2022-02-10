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
import { restGet } from "../restful/restful";

export default {
  name: "NotebooksPage",
  components: { ContainerPage, NotebookBazaarViewCards },
  data() {
    return {
      loading: true,
      notebooksViewedByUser: null,
      polling: null,
    };
  },
  computed: {
    user() { return this.$store.getters.getCurrentUser()}
  },

  methods: {
    getBazaarData() {
      restGet(`/api/bazaar`).then(
        (res) => {
          this.notebooksViewedByUser = res
        }
      )
    },
    fetchData() {
      this.loading = true
      this.getBazaarData();
      this.loading = false;
    },
     pollData() {
      const intervalTime = 5000;
      this.polling = setInterval(() => {
        this.getBazaarData();
      }, intervalTime);
    },
  },
  mounted() {
    this.fetchData();
    this.pollData();
  },
  unmounted() {
    clearInterval(this.polling);
  },
};
</script>
