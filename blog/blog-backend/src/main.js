require('dotenv').config()
import Koa from 'koa'
import Router from 'koa-router'
import bodyParser from 'koa-bodyparser'
import mongoose from 'mongoose'

const {PORT, MONGO_URI} = process.env;

import api from './api'

mongoose.connect(MONGO_URI)
  .then(() => {
    console.log('Connect to MongoDB')
  })
  .catch((e) => {
    console.error(e)
  });

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