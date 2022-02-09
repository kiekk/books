<template>
  <div style="height:1000px">
    <p v-notification:top="100">고정 자리에 표시하기</p>
  </div>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'

export default {
  name: 'App',
  components: {
    HelloWorld,
  },
}
</script>
