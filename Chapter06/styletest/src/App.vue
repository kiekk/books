<template>
  <div id="app">
    <h2>{{msg}}</h2>
    <child1 />
    <child2 />
    <module1 />
  </div>
</template>

<script>
import Child1 from './components/Child1.vue'
import Child2 from './components/Child2.vue'
import Module1 from './components/Module1.vue'

export default {
  name: 'app',
  components: {
    Child1, Child2, Module1
  },
  data(){
    return {
      msg : 'Welcome to Your Vue.js'
    }
  }
}
</script>

<style>
</style>
