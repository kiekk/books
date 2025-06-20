<template>
  <div>
    <search placeholder="두 글자 이상 입력 후 엔터"></search>
    <contact-list></contact-list>
  </div>
</template>

<script>
import ContactList from './components/ContactList.vue'
import Search from './components/Search.vue'

export default {
  name: 'app',
  components: {
    Search, ContactList
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
