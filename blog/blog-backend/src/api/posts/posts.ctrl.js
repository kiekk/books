import Post from '../../models/posts'

/*
  POST /api/posts
  {
    title: '제목',
    body: '내용',
    tags: ['태그1', '태그2']
  }
 */
export const write = async (ctx) => {
  const { title, body, tags } = ctx.request.body
  const post = new Post({
    title,
    body,
    tags,
  })

  try {
    await post.save()
    ctx.body = post
  } catch (e) {
    ctx.throw(500, e)
  }
}

/*
  GET /api/posts
 */
export const list = async (ctx) => {
  try {
    const posts = await Post.find().exec()
    ctx.body = posts
  } catch (e) {
    ctx.throw(500, e)
  }
}

/*
  GET /api/posts/:id
 */
export const read = async (ctx) => {
  const { id } = ctx.params
  try {
    const post = await Post.findById(id).exec()
    if (!post) {
      ctx.status = 400 // Not Found
      return
    }
    ctx.body = post
  } catch (e) {
    ctx.throw(500, e)
  }
}

/*
  DELETE /api/posts/:id
 */
export const remove = async (ctx) => {
  const { id } = ctx.params
  try {
    await Post.findByIdAndRemove(id).exec()
    ctx.status = 204 // No Content (성공했지만 응답 데이터는 없음)
  } catch (e) {
    ctx.throw(500, e)
  }
}

export const update = (ctx) => {}
