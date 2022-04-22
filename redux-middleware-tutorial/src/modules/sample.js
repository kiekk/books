import {handleActions} from 'redux-actions'
import * as api from '../lib/api'

// 한 API 요청당 세 개의 액션 타입을 만들어야 합니다.
// 요청, 성공, 실패
const GET_POST = 'sample/GET_POST'
const GET_POST_SUCCESS = 'sample/GET_POST_SUCCESS'
const GET_POST_FAILURE = 'sample/GET_POST_FAILURE'

const GET_USERS = 'sample/GET_USERS'
const GET_USERS_SUCCESS = 'sample/GET_USERS_SUCCESS'
const GET_USERS_FAILURE = 'sample/GET_USERS_FAILURE'

// thunk 함수 생성

export const getPost = id => async dispatch => {
  dispatch({type: GET_POST})
  try {
    const response = await api.getPost(id)
    dispatch({
      type: GET_POST_SUCCESS,
      payload: response.data
    }) // 요청 성공
  } catch (e) {
    dispatch({
      type: GET_POST_FAILURE,
      payload: e,
      error: true
    })  // 에러 발생
    throw e
  }
}

export const getUsers = () => async dispatch => {
  dispatch({type: GET_USERS})
  try {
    const response = await api.getUsers()
    dispatch({
      type: GET_USERS_SUCCESS,
      payload: response.data
    })
  } catch (e) {
    dispatch({
      type: GET_USERS_FAILURE,
      payload: e,
      error: true
    })
    throw e
  }
}

// 초기 상태 선언
const initialState = {
  loading: {
    GET_POST: false,
    GET_USERS: false
  },
  post: null,
  users: null
}

const sample = handleActions(
  {
    [GET_POST]: state => ({
      ...state,
      loading: {
        ...state.loading,
        GET_POST: true
      }
    }),
    [GET_POST_SUCCESS]: (state, action) => ({
      ...state,
      loading: {
        ...state.loading,
        GET_POST: false
      },
      post: action.payload
    }),
    [GET_POST_FAILURE]: (state, action) => ({
      ...state,
      loading: {
        ...state.loading,
        GET_POST: false
      }
    }),
    [GET_USERS]: state => ({
      ...state,
      loading: {
        ...state.loading,
        GET_USERS: true
      }
    }),
    [GET_USERS_SUCCESS]: (state, action) => ({
      ...state,
      loading: {
        ...state.loading,
        GET_USERS: false
      },
      users: action.payload
    }),
    [GET_USERS_FAILURE]: (state, action) => ({
      ...state,
      loading: {
        ...state.loading,
        GET_USERS: false
      }
    })
  },
  initialState
)

export default sample;