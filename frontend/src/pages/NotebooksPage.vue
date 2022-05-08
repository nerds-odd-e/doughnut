<template>
  <NoteToolbar @note-realm-updated="fetchData"/>
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
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NoteToolbar from "../components/toolbars/NoteToolbar.vue";
import NotebookSubscriptionCards from "../components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useLoadingApi from "../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  name: "NotebooksPage",
  components: {
    ContainerPage,
    NoteToolbar,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
  },
  data() {
    return {
      subscriptions: undefined as Generated.NotebookSubscription[] | undefined,
      notebooks: undefined as Generated.NotebookViewedByUser[] | undefined,
    };
  },
  methods: {
    fetchData() {
      this.api.getNotebooks().then((res) => {
        this.notebooks = res.notebooks;
        this.subscriptions = res.subscriptions;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
