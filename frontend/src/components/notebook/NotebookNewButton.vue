<template>
  <PopupButton title="New notebook">
    <template v-slot:face>
      <slot />
    </template>
    <NotebookNewDialog :circle="circle"/>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NotebookNewDialog from "./NotebookNewDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
    props: { circle: Object },
    components: { PopupButton, NotebookNewDialog }
});
</script>
