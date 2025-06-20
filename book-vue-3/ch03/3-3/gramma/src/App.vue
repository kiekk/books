<template>
  <img alt="Vue logo" src="./assets/logo.png"/>
  <HelloWorld msg="Hello Vue 3.0 + Vite"/>
  <p>Total : {{ total }}</p>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'

const exMixin = {
  data() {
    return {
      total: 5,
      data: [1, 2, 3, 4, 5],
    }
  },
}

export default {
  name: 'App',
  mixins: [exMixin],
  components: {
    HelloWorld,
  },
  data() {
    return {
      // mixins의 변수명과 겹칠 경우 data의 변수가 먼저 적용
      total: 10,
    }
  },
}
</script>
