<template>
    <div class="row">
        <div :class="`col-12 col-sm-6 col-md-${12/(columns-1)} col-lg-${12/columns}`" v-for="note in notes" :key="note.id">
            <Card :note="note" :linkFragment="linkFragment">
                <template #cardHeader>
                    <slot name="cardHeader" :note="note"/>
                </template>
                <template #button>
                    <slot name="button" :note="note"/>
                </template>
            </Card>
        </div>
    </div>
</template>

<script>
import Card from "./Card.vue"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
import { defineProps } from "vue"

export default {
  props: {
      notes: Array, linkFragment: {type: Object, default: NoteTitleWithLink}, columns: {type: Number, default: 4}
  },
  components: { Card, NoteTitleWithLink }
}
</script>
