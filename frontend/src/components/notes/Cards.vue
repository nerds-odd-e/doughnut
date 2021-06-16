<template>
    <div class="row">
        <div class="col-12 col-sm-6 col-md-4 col-lg-3" v-for="note in notes" :key="note.id">
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

<script setup>
import Card from "./Card.vue"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
import { defineProps } from "vue"

const props = defineProps({notes: Array, linkFragment: {type: Object, default: NoteTitleWithLink}})
</script>
