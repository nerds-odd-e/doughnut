<template>
<div>
    <Cards :notes="notes">
         <template #cardHeader="{note}">
               <span class="card-header d-flex flex-row-reverse p-0">
                    <slot name="headerButtons" :note="note"/>
               </span>
         </template>
         <template #button="{note}">
               <slot name="button" :note="note"/>
         </template>
    </Cards>
</div>
</template>

<script setup>
import Cards from "./Cards.vue"
import { ref, defineProps } from "vue"

const props = defineProps({notes: Array})
</script>
