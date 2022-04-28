import Router from 'koa-router'
import * as postsCtrl from './posts.ctrl'
const posts = new Router();

posts.get('/', postsCtrl.list);
posts.post('/', postsCtrl.write);
posts.get('/:id', postsCtrl.read);

export default posts;