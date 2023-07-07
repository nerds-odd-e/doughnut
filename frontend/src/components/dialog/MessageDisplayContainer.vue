<template>
  <div id="messageDisplayContainer">
    <li v-for="msg in messages" :key="msg.content">
      {{ msg.content }}
    </li>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Message } from "../../store/DialogMessage";

export default defineComponent({
  props: {
    messages: { type: Object as PropType<Message[]> },
  },
});
</script>
