import Router from 'koa-router'
import * as postsCtrl from './posts.ctrl'
const posts = new Router();

posts.post('/', postsCtrl.write);

export default posts;