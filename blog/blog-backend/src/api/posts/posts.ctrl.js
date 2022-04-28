import Post from "../../models/post";

export const write = async ctx => {
  const {title, body, tags} = ctx.request.body;

  const post = new Post({
    title,
    body,
    tags
  });

  try {
    await post.save();
    ctx.body = post;
  } catch (e) {
    ctx.throw(500, e);
  }
}

export const list = async ctx => {
  try {
    ctx.body = await Post.find().exec();
  } catch (e) {
    ctx.throw(500, e);
  }
}

export const read = async ctx => {
  const {id} = ctx.params;
  try {
    const post = await Post.findById(id).exec();
    if (!post) {
      ctx.status = 404;
      return;
    }
    ctx.body = post;
  } catch (e) {
    ctx.throw(500, e);
  }
}