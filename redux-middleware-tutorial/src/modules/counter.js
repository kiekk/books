import {createAction, handleActions} from 'redux-actions'

const INCREASE = 'counter/INCREASE'
const DECREASE = 'counter/DECREASE'

export const increase = createAction(INCREASE)
export const decrease = createAction(DECREASE)

// 1초 뒤 increase or decrease 함수를 디스패치
export const increaseAsync = () => dispatch => {
  setTimeout(() => {
    dispatch(increase())
  }, 1000)
}

export const decreaseAsync = () => dispatch => {
  setTimeout(() => {
    dispatch(decrease())
  }, 1000)
}

// 초기 상태
const initialState = 0

const counter = handleActions(
  {
    [INCREASE]: state => state + 1,
    [DECREASE]: state => state - 1
  },
  initialState,
)

export default counter