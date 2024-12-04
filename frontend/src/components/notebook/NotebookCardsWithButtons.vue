<template>
  <div class="row">
    <div
      class="col-12 col-sm-6 col-md-4 col-lg-3"
      v-for="notebook in notebooks"
      :key="notebook.id"
    >
      <Card :noteTopology="notebook.headNoteTopic">
        <template #cardHeader>
          <span class="card-header d-flex flex-row-reverse p-0">
            <slot :notebook="notebook" />
          </span>
        </template>
      </Card>
    </div>
  </div>
</template>

<script lang="ts">
import type { Notebook } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import Card from "../notes/Card.vue"

export default defineComponent({
  props: {
    notebooks: Array as PropType<Array<Notebook>>,
  },
  components: { Card },
})
</script>
