import styled from 'styled-components'

/**
 * 회원가입, 로그인 페이지의 레이아웃
 */

const AuthTemplateBlock = styled.div``

const AuthTemplate = ({ children }) => {
  return <AuthTemplateBlock>{children}</AuthTemplateBlock>
}

export default AuthTemplate
