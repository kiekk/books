import { createAction } from 'redux-actions'

// 액션 정의
const INCREASE = 'counter/INCREASE'
const DECREASE = 'counter/DECREASE'

// createAction을 사용하면 매번 객체를 직접 만들지 않아도 됨
export const increase = createAction(INCREASE)
export const decrease = createAction(DECREASE)

// 초기 상태
const initialState = {
    number: 0
}

function counter(state = initialState, action) {
    switch (action.type) {
        case INCREASE:
            return {
                number: state.number + 1
            }
        case DECREASE:
            return {
                number: state.number - 1
            }
        default:
            return state
    }
}

export default counter