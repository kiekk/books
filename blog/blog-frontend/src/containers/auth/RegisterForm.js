import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { changeField, initializeForm, register } from '../../modules/auth'
import AuthForm from '../../components/auth/AuthForm'
import { check } from '../../modules/users'

const RegisterForm = () => {
  const dispatch = useDispatch()
  const { form, auth, authError, user } = useSelector(({ auth, user }) => ({
    form: auth.register,
    auth: auth.auth,
    authError: auth.authError,
    user: user.user,
  }))
  // input change event handler
  const onChange = (e) => {
    const { value, name } = e.target
    dispatch(
      changeField({
        form: 'register',
        key: name,
        value,
      }),
    )
  }
  // form register event handler
  const onSubmit = (e) => {
    e.preventDefault()

    const { username, password, passwordConfirm } = form

    if (password !== passwordConfirm) {
      // TODO: 오류 처리
      return
    }
    dispatch(register({ username, password }))
  }

  // 컴포넌트 처음 렌더링될 때 form 초기화
  useEffect(() => {
    dispatch(initializeForm('register'))
  }, [dispatch])

  // 회원가입 성공/실패 처리
  useEffect(() => {
    if (authError) {
      console.log('오류 발생')
      console.log(authError)
      return
    }
    if (auth) {
      console.log('회원가입 성공')
      console.log(auth)
      dispatch(check())
    }
  }, [auth, authError, dispatch])

  // user 값 설정 확인
  useEffect(() => {
    if (user) {
      console.log('check API 성공')
      console.log(user)
    }
  }, [user])

  return (
    <AuthForm
      type="register"
      form={form}
      onChange={onChange}
      onSubmit={onSubmit}
    />
  )
}

export default RegisterForm
