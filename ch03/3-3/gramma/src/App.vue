<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <reactive-variable/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import ReactiveVariable from "./components/ReactiveVariable.vue";

export default {
  name: 'App',
  components: {
    ReactiveVariable,
    HelloWorld
  },
}
</script>
