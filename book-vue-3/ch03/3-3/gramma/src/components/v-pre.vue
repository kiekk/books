<template>
  <div>{{ msg }}</div>
  <!--
    v-pre : 컴파일 X
  -->
  <div v-pre>{{ msg }}</div>
</template>

<script>
export default {
  data() {
    return {
      msg: 'test msg'
    }
  }
}
</script>