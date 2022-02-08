<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <computed />
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import Computed from "./components/Computed.vue";

export default {
  name: 'App',
  components: {
    HelloWorld,
    Computed
  },
}
</script>
