const divToggle = document.querySelector('.toggle')
const counter = document.querySelector('h1')
const btnIncrease = document.querySelector('#increase')
const btnDecrease = document.querySelector('#decrease')

// 액션 이름 정의
const TOGGLE_SWITCH = 'TOGGLE_SWITCH'
const INCREASE = 'INCREASE'
const DECREASE = 'DECREASE'

// 액션 이름을 사용해 액션 객체를 생성하는 함수 작성
const toggleSwitch = () => ({ type: TOGGLE_SWITCH})
const increase = difference => ({ type: INCREASE, difference})
const decrease = () => ({ type: DECREASE })

// 초기값 설정
const initialState = {
    toggle: false,
    counter: 0
}