<template>
  <div id="app">
    <region-buttons></region-buttons>
    <country-list></country-list>
  </div>
</template>

<script>
import RegionButtons from './components/RegionButtons'
import CountryList from './components/CountryList'

export default {
  name: 'app',
  components: { RegionButtons, CountryList }
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
