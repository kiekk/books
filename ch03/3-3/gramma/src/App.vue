<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <non-props msg="RootNode Example" />
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import NonProps from './components/non-props.vue'

export default {
  name: 'App',
  components: {
    HelloWorld,
    NonProps
  },
}
</script>
