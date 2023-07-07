<template>
  <div id="messageDisplayContainer">
    <div
      class="messageDiv"
      :style="{
        'text-align': setTextAlign(msg.role),
      }"
      v-for="msg in messages"
      :key="msg.content"
    >
      {{ msg.content }}
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Message } from "../../store/DialogMessage";

export default defineComponent({
  props: {
    messages: { type: Object as PropType<Message[]> },
  },
  methods: {
    setTextAlign(role: string) {
      if (role === "User") {
        return "right";
      }

      return "left";
    },
  },
});
</script>

<style lang="scss" scoped>
#messageDisplayContainer {
  width: 100%;
  height: 200px;
  border-style: solid;
  overflow-y: auto;
  margin: 10px;
}
div.messageDiv {
  padding: 2px;
}
</style>
