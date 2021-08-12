import Router from 'koa-router'
import posts from './posts'
import auth from './auth'

const api = new Router()

api.use('/posts', posts.routes())
api.user('/auth', auth.routes())

export default api
