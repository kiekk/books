import HeaderContainer from '../containers/common/HeaderContainer'
import PostListContainer from '../containers/posts/PostListContainer'
import PaginationContainer from '../containers/post/PaginationContainer'

const PostListPage = () => {
  return (
    <>
      <HeaderContainer />
      <PostListContainer />
      <PaginationContainer />
    </>
  )
}

export default PostListPage
