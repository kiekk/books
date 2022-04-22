import {createAction, handleActions} from 'redux-actions'

const INCREASE = 'counter/INCREASE'
const DECREASE = 'counter/DECREASE'

export const increase = createAction(INCREASE)
export const decrease = createAction(DECREASE)

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