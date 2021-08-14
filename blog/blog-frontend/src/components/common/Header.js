import styled from 'styled-components'
import Responsive from './Responsive'
import Button from './Button'
import { Link } from 'react-router-dom'

const HeaderBlock = styled.div`
  position: fixed;
  width: 100%;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
`

/**
 * Responsive 컴포넌트의 속성에 스타일을 추가해서 새로운 컴포넌트 생성
 */
const Wrapper = styled(Responsive)`
  height: 4rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  .logo {
    font-size: 1.125rem;
    font-weight: 800;
    letter-spacing: 2px;
  }
  .right {
    display: flex;
    align-items: center;
  }
`

const UserInfo = styled.div`
  font-weight: 800;
  margin-right: 1rem;
`

/**
 * 헤더가 fixed이기 때문에 페이지 콘텐츠가 4rem 아래에 나타나도록 하는 컴포넌트
 */
const Spacer = styled.div`
  height: 4rem;
`

const Header = ({ user }) => {
  return (
    <>
      <HeaderBlock>
        <Wrapper>
          <Link to="/" className="logo">
            REACTERS
          </Link>
          {user ? (
            <Link className="right">
              <Button to="/logout">로그아웃</Button>
            </Link>
          ) : (
            <Link className="right">
              <Button to="/login">로그인</Button>
            </Link>
          )}
        </Wrapper>
      </HeaderBlock>
      <Spacer />
    </>
  )
}

export default Header
