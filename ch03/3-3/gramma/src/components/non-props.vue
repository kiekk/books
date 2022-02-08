<template>
  <div>
    <p>@Component: {{ $attrs.msg }}</p>
  </div>
</template>

<script>
export default {
  setup(props, context) {
    const msg = context.attrs.msg
    return {
      msg,
    }
  },
}
</script>