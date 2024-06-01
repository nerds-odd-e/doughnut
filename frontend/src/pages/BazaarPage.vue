<template>
  <ContainerPage
    v-bind="{
      contentExists: !!bazaarNotebooks,
      title: 'Welcome To The Bazaar',
    }"
  >
    <p>These are shared notes from doughnut users.</p>
    <div v-if="bazaarNotebooks">
      <NotebookBazaarViewCards
        :bazaar-notebooks="bazaarNotebooks"
        :logged-in="!!user"
      />
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { BazaarNotebook, User } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import NotebookBazaarViewCards from "@/components/bazaar/NotebookBazaarViewCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  components: { ContainerPage, NotebookBazaarViewCards },
  props: {
    user: {
      type: Object as PropType<User>,
      required: false,
    },
  },
  data() {
    return {
      bazaarNotebooks: null as BazaarNotebook[] | null,
    };
  },

  methods: {
    fetchData() {
      this.managedApi.restBazaarController.bazaar().then((res) => {
        this.bazaarNotebooks = res;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
