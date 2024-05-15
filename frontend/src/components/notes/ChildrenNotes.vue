<template>
  <div class="row">
    <div class="col-auto bg-light p-0" style="width: 40px">
      <button
        class="btn btn-sm"
        v-if="internalExpandChildren"
        role="button"
        title="collapse children"
        @click="collapse()"
      >
        <SvgCollapse />
      </button>
      <button
        class="btn btn-sm"
        v-else
        role="button"
        title="expand children"
        @click="expand()"
      >
        <SvgExpand />
      </button>
    </div>
    <div class="col">
      <div class="row">
        <div v-if="!internalExpandChildren">
          <div role="collapsed-children-count">{{ notes.length }}</div>
        </div>
        <div v-else v-for="note in notes" :key="note.id">
          <Card v-bind="{ note }">
            <template #cardHeader>
              <slot name="cardHeader" :note="note" />
            </template>
            <template #button>
              <slot name="button" :note="note" />
            </template>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { PropType, defineComponent, ref } from "vue";
import { Note } from "@/generated/backend";
import Card from "./Card.vue";
import SvgCollapse from "../svgs/SvgCollapse.vue";
import SvgExpand from "../svgs/SvgExpand.vue";

export default defineComponent({
  setup(props) {
    return {
      internalExpandChildren: ref(props.expandChildren),
    };
  },
  props: {
    notes: { type: Array as PropType<Note[]>, required: true },
    expandChildren: { type: Boolean, required: true },
  },
  components: { Card, SvgCollapse, SvgExpand },
  methods: {
    collapse() {
      this.internalExpandChildren = false;
    },
    expand() {
      this.internalExpandChildren = true;
    },
  },
});
</script>
