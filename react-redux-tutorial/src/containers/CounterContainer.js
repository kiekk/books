import React from 'react'
import {connect} from 'react-redux'
import Counter from '../components/Counter'
import {increase, decrease} from '../modules/counter'

const CounterContainer = ({number, increase, decrease}) => {
    return (
        <Counter number={number} onIncrease={increase} onDecrease={decrease}/>
    )
}

export default connect(
    state => ({
        number: state.counter.number,
    }),
    // bindActionCreators 생략 후 객체 형태로 넣어주면
    // connect 함수에서 내부적으로 bindActionCreators 작업을 대신해줌
    {
        increase,
        decrease
    },
)(CounterContainer)