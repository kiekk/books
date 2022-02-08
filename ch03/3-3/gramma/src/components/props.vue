<template>
  <h3 v-text="title"/>
  <span v-for="i in data" :key="i">{{ i }}</span>
</template>

<script>
import { ref } from 'vue'

export default {
  setup() {
    const count = ref(0)
    return { count }
  },
  props: {
    title: {
      type: String,
      required: true,
      validator: function (value) {
        return typeof value == String
      },
    },
    data: {
      type: Object,
      default: () => {
        return []
      },
    },
  },
}
</script>