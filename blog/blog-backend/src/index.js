require('dotenv').config()
const Koa = require('koa')
const Router = require('koa-router')
const bodyParser = require('koa-bodyparser')

const { PORT } = process.env;

const api = require('./api')

const app = new Koa();
const router = new Router();

router.use('/api', api.routes())

// router 적용 전에 bodyParser 적용
app.use(bodyParser())

app.use(router.routes()).use(router.allowedMethods())

const port = PORT || 4000;
app.listen(port, () => {
  console.log(`Listening to port ${port}`)
});