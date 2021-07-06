<template>
  <h2>Notebooks</h2>
  <LoadingPage v-bind="{loading, contentExists: !!notebooksViewedByUser}">
    <div v-if="!!notebooksViewedByUser">
      <p> <router-link class="nav-link" :to="{name: 'notebookNew'}">Add New Notebook</router-link> </p>
      <NotebookViewCards :notebooks="notebooksViewedByUser.notebooks"/>
      <h2>Subscribed Notes</h2>
      <div th:replace="_fragments/subscription_fragments :: noteSubscriptionCards(${subscriptions})"/>
      <div id="app"></div>
    </div>
  </LoadingPage>
</template>

<script>
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue"
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet} from "../restful/restful"

export default {
  name: 'NotebooksPage',
  components: { LoadingPage, NotebookViewCards },
  data() {
    return {
      loading: false,
      notebooksViewedByUser: null,
    }
  },
  methods: {
    fetchData() {
      restGet(
        `/api/notebooks`,
        r=>this.loading=r,
        (res) => this.notebooksViewedByUser = res
      )
    }
  },
  mounted() {
    this.fetchData()
  }
}
</script>
