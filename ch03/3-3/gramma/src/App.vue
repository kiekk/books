<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <v-if/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import VIf from './components/v-if.vue'

export default {
  name: 'App',
  components: {
    HelloWorld,
    VIf
  },
}
</script>
