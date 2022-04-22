export default function createRequestThunk(type, request) {
  // 성공, 실패 액션 타입 정의
  const SUCCESS = `${type}_SUCCESS`
  const FAILURE = `${type}_FAILURE`

  return (params) => async (dispatch) => {
    dispatch({ type }) // 시작
    try {
      const response = await request(params)
      dispatch({
        type: SUCCESS,
        payload: response.data,
      }) // 성공
    } catch (e) {
      dispatch({
        type: FAILURE,
        payload: e,
      }) // 에러
      throw e
    }
  }
}

// 사용법
// createRequestThunk('GET_USERS', api.getUsers)
