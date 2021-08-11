const Koa = require('koa')

const app = new Koa()

app.use((ctx, next) => {
  console.log(ctx.url)
  console.log(1)

  if (ctx.query.authorized !== '1') {
    ctx.status = 401 // Unauthorized
    return
  }
  /*
    next가 반환하는 Promise는 다음에 처리해야 할 미들웨어가 끝나야 완료
   */
  next().then(() => {
    console.log('END')
  })
})

app.use((ctx, next) => {
  console.log(2)
  next()
})

app.use((ctx) => {
  ctx.body = 'hello world'
})

app.listen(4000, () => {
  console.log('Listening to port 4000')
})
