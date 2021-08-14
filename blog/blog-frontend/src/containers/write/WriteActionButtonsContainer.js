import { useEffect } from 'react'
import WriteActionButtons from '../../components/write/WriteActionButtons'
import { useSelector, useDispatch } from 'react-redux'
import { withRouter } from 'react-router-dom'
import { writePost } from '../../modules/write'

const WriteActionButtonsContainer = ({ history }) => {
  const dispatch = useDispatch()
  const { title, body, tags, post, postError } = useSelector(({ write }) => ({
    title: write.title,
    body: write.body,
    tags: write.tags,
    post: write.post,
    postError: write.postError,
  }))

  // 포스트 등록
  const onPublish = () => {
    dispatch(
      writePost({
        title,
        body,
        tags,
      }),
    )
  }

  // 취소
  const onCancel = () => {
    history.goBack()
  }

  // 성공 / 실패 시 작업
  useEffect(() => {
    if (post) {
      const { _id, user } = post
      history.push(`/@${user.username}/${_id}`)
    }
    if (postError) {
      console.error(postError)
    }
  }, [history, post, postError])

  return <WriteActionButtons onPublish={onPublish} onCancel={onCancel} />
}

export default withRouter(WriteActionButtonsContainer)
