<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <v-for />
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import VFor from './components/v-for.vue'

export default {
  name: 'App',
  components: {
    HelloWorld,
    VFor
  },
}
</script>
