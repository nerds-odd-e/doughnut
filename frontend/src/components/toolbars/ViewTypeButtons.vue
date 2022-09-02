<template>
  <ViewTypeButton
    class="btn active btn-outline-secondary"
    id="viewTypeSelect"
    data-bs-toggle="dropdown"
    data-toggle="dropdown"
    aria-haspopup="true"
    aria-expanded="false"
    :view-type="viewType"
    role="button"
    title="view type"
  />
  <div class="dropdown-menu" aria-labelledby="viewTypeSelect">
    <ViewTypeButton
      class="dropdown-item"
      role="button"
      v-for="(type, index) in options"
      :view-type="type.value"
      :title="type.title"
      @click="viewTypeChange(type.value)"
      :key="index"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { routeNameForViewType, viewTypeNames } from "../../models/viewTypes";
import ViewTypeButton from "./ViewTypeButton.vue";

export default defineComponent({
  props: {
    noteId: Number,
    viewType: String,
  },
  components: {
    ViewTypeButton,
  },

  computed: {
    options() {
      return viewTypeNames.map((name) => ({
        value: name,
        title: `${name} view`,
      }));
    },
  },
  methods: {
    viewTypeChange(newType) {
      this.$router.push({
        name: routeNameForViewType(newType),
        params: { noteId: this.noteId },
      });
    },
  },
});
</script>
