<template>
  <div class="container pt-5">
    <div class="row no-gutters"
         v-for="(d, index) in user_data.resume"
         :key="index"
    >
      <Card :top="!index" :resume="d" :show="index % 2 === 1"/>
      <div class="col-sm-2"></div>
      <Card :top="!index" :resume="d" :show="index % 2 === 0"/>
    </div>
  </div>
</template>

<script>
import {defineComponent} from "vue";

const Car = defineComponent({
  props: {
    top: Boolean,
    resume: Object,
    show: Boolean,
  },
  setup(props) {
  },
  template: `
    <div v-if="show" class="col-sm py-2">
    <div class="card" :class="{'border-primary bg-primary text-light': top, shadow: top}">
      <div class="card-body">
        <h4 class="card-title" :class="[top ? 'text-light' : 'text-muted']">
          {{ resume.title }}
        </h4>
        <hr/>
        <p class="card-text">
          {{ resume.content }}
        </p>
        <a v-if="resume.URL != 'null'" :href="resume.URL" target="_blank" :class="[top ? 'text-light': 'text-primary']">link</a>
      </div>
    </div>
    </div>
    <div v-if="!show" class="col-sm"></div>
  `,
})
</script>