import Counter from "../components/Counter";
import {connect} from 'react-redux'
import {increase, decrease} from "../modules/counter";

const CounterContainer = ({ number, increase, decrease }) => {
  return <Counter number={number} onIncrease={increase} onDecrease={decrease}/>
}

export default connect(
  state => ({
    number: state.counter.number
  }),
  dispatch => ({
    increase: () => {
      dispatch(increase())
    },
    decrease: () => {
      dispatch(decrease())
    }
  })
)(CounterContainer);