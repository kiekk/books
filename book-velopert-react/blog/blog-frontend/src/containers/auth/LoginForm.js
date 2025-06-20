import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { changeField, initializeForm, login } from '../../modules/auth'
import AuthForm from '../../components/auth/AuthForm'
import { withRouter } from 'react-router-dom'
import { check } from '../../modules/user'

const LoginForm = ({ history }) => {
  const [error, setError] = useState(null)
  const dispatch = useDispatch()
  const { form, auth, authError, user } = useSelector(({ auth, user }) => ({
    form: auth.login,
    auth: auth.auth,
    authError: auth.authError,
    user: user.user,
  }))
  // input change event handler
  const onChange = (e) => {
    const { value, name } = e.target
    dispatch(
      changeField({
        form: 'login',
        key: name,
        value,
      }),
    )
  }
  // form register event handler
  const onSubmit = (e) => {
    e.preventDefault()

    const { username, password } = form
    dispatch(login({ username, password }))
  }

  // 컴포넌트 처음 렌더링될 때 form 초기화
  useEffect(() => {
    dispatch(initializeForm('login'))
  }, [dispatch])

  useEffect(() => {
    if (authError) {
      console.log('오류 발생')
      console.log(authError)
      setError('로그인 실패')
      return
    }
    if (auth) {
      console.log('로그인 성공')
      dispatch(check())
    }
  }, [auth, authError, dispatch])

  useEffect(() => {
    if (user) {
      history.push('/')
      try {
        localStorage.setItem('user', JSON.stringify(user))
      } catch (e) {
        console.log('localStorage is not working')
      }
    }
  }, [history, user])

  return (
    <AuthForm
      type="login"
      form={form}
      onChange={onChange}
      onSubmit={onSubmit}
      error={error}
    />
  )
}

export default withRouter(LoginForm)
