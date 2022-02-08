<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <props title="Component 1" :data="[2,4,6]" />
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import props from "./components/props.vue";

export default {
  name: 'App',
  components: {
    HelloWorld,
    props
  },
}
</script>
