<template>
  <BreadcrumbWithCircle v-bind="{ ancestors: [] }" />
  <NotebooksToolbar @note-realm-updated="fetchData" />
  <ContainerPage v-bind="{ loading, contentExists: true, title: 'Notebooks' }">
    <p>
      <NotebookNewButton>Add New Notebook</NotebookNewButton>
    </p>
    <NotebookViewCards :notebooks="notebooks" />
    <h2>Subscribed Notes</h2>
    <NotebookSubscriptionCards
      :subscriptions="subscriptions"
      @updated="fetchData()"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue";
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NotebooksToolbar from "../components/toolbars/NotebooksToolbar.vue";
import NotebookSubscriptionCards from "../components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: { updatedAt: Date },
  name: "NotebooksPage",
  components: {
    ContainerPage,
    NotebooksToolbar,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
    BreadcrumbWithCircle,
  },
  data() {
    return {
      subscriptions: undefined as Generated.Subscription[] | undefined,
      notebooks: undefined as Generated.NotebookViewedByUser[] | undefined,
    };
  },
  methods: {
    fetchData() {
      this.api.notebookMethods.getNotebooks().then((res) => {
        this.notebooks = res.notebooks;
        this.subscriptions = res.subscriptions;
      });
    },
  },
  watch: {
    updatedAt() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
