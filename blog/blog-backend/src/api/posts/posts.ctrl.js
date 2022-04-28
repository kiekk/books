let postId = 1 // id 초기값

// posts 배열 초기 데이터
const posts = [
  {
    id: 1,
    title: '제목',
    body: '내용',
  },
]

/*
  포스트 작성
  POST /api/posts
  { title, body }
 */
export const write = (ctx) => {
  // REST API의 Request Body는 ctx.request.body로 조회
  const { title, body } = ctx.request.body
  postId += 1 // 기존 postId에 1 더하기
  const post = { id: postId, title, body }
  posts.push(post)
  ctx.body = post
}

/*
  포스트 목록 조회
  GET /api/posts
 */
export const list = (ctx) => {
  ctx.body = posts
}

/*
  특정 포스트 조회
  GET /api/posts/:id
 */
export const read = (ctx) => {
  const { id } = ctx.params
  // 주어진 id로 포스트 찾기
  const post = posts.find((p) => p.id.toString() === id)
  // 포스트가 없으면 오류 반환
  if (!post) {
    ctx.status = 400
    ctx.body = {
      message: '포스트가 존재하지 않습니다.',
    }
    return
  }
  ctx.body = post
}

/*
  특정 포스트 제거
  DELETE /api/posts/:id
 */
export const remove = (ctx) => {
  const { id } = ctx.params
  // 주어진 id에 해당하는 포스트 index 찾기
  const index = posts.findIndex((p) => p.id.toString() === id)
  // 포스트가 없으면 오류 반환
  if (!post) {
    ctx.status = 400
    ctx.body = {
      message: '포스트가 존재하지 않습니다.',
    }
    return
  }
  // 조회한 index로 해당 포스트 제거
  posts.splice(index, 1)
  ctx.status = 204 // No Content
}

/*
  포스트 수정
  PUT /api/posts/:id
  { title, body }
  PUT 메소드는 데이터를 통째로 교환
 */
export const replace = (ctx) => {
  const { id } = ctx.params
  // 주어진 id에 해당하는 포스트 index 찾기
  const index = posts.findIndex((p) => p.id.toString() === id)
  // 포스트가 없으면 오류 반환
  if (!post) {
    ctx.status = 400
    ctx.body = {
      message: '포스트가 존재하지 않습니다.',
    }
    return
  }
  // 전체 객체를 덮어 씌움
  posts[index] = {
    id,
    ...ctx.request.body,
  }
  ctx.body = posts[index]
}

/*
  포스트 수정
  PATCH /api/posts/:id
  PATCH 메소드는 데이터를 일부분만 교환
  { title, body }
 */
export const update = (ctx) => {
  const { id } = ctx.params
  // 주어진 id에 해당하는 포스트 index 찾기
  const index = posts.findIndex((p) => p.id.toString() === id)
  // 포스트가 없으면 오류 반환
  if (!post) {
    ctx.status = 400
    ctx.body = {
      message: '포스트가 존재하지 않습니다.',
    }
    return
  }
  // 기존 값에 정보를 덮어 씌움
  posts[index] = {
    ...posts[index],
    ...ctx.request.body,
  }
  ctx.body = posts[index]
}
