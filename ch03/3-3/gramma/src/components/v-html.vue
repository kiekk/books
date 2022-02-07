<template>
  <div v-html="tag"></div>
</template>

<script>
export default {
  data() {
    return {
      tag: '<i>HTML TEXT</i>'
    }
  }
}
</script>