<template>
  <ContainerPage v-bind="{ contentExists: true, title: 'Notebooks' }">
    <p>
      <NotebookNewButton>Add New Notebook</NotebookNewButton>
    </p>
    <main>
      <NotebookViewCards :notebooks="notebooks" />
    </main>
    <h2>Subscribed Notes</h2>
    <NotebookSubscriptionCards
      :subscriptions="subscriptions"
      @updated="fetchData()"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { NotebookViewedByUser, Subscription } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import NotebookViewCards from "@/components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue";
import NotebookSubscriptionCards from "@/components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNoteId: Number,
  },
  name: "NotebooksPage",
  components: {
    ContainerPage,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
  },
  data() {
    return {
      subscriptions: undefined as Subscription[] | undefined,
      notebooks: undefined as NotebookViewedByUser[] | undefined,
    };
  },
  methods: {
    fetchData() {
      this.managedApi.restNotebookController.myNotebooks().then((res) => {
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
