import Router from 'koa-router'
import * as postsCtrl from './posts.ctrl'
const posts = new Router();
import checkLoggedIn from "../../lib/checkLoggedIn";

posts.get('/', postsCtrl.list);
posts.post('/', checkLoggedIn, postsCtrl.write);
posts.get('/:id', postsCtrl.checkObjectId, postsCtrl.read);
posts.delete('/:id', checkLoggedIn, postsCtrl.checkObjectId, postsCtrl.remove);
posts.patch('/:id', checkLoggedIn, postsCtrl.checkObjectId, postsCtrl.update);

export default posts;