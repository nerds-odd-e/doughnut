<template>
  <BazaarNotebookButtons :notebook="notebook" />
</template>

<script>
import BazaarNotebookButtons from "./BazaarNotebookButtons.vue";

export default {
  props: { note: Object, notebook: Object },
  components: { BazaarNotebookButtons },
};
</script>
