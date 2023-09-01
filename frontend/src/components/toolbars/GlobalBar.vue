<template>
  <ToolbarFrame>
    <PopButton title="choose a circle" :sidebar="'left'">
      <template #button_face>
        <SvgForward />
      </template>
      <CircleSelector />
    </PopButton>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PopButton from "../commons/Popups/PopButton.vue";
import SvgForward from "../svgs/SvgForward.vue";
import CircleSelector from "../circles/CircleSelector.vue";
import ToolbarFrame from "./ToolbarFrame.vue";

export default defineComponent({
  components: {
    PopButton,
    SvgForward,
    CircleSelector,
    ToolbarFrame,
  },
});
</script>
