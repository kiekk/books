import {Component} from 'react'

class Counter extends Component {
  state = {
    number: 0,
    fixedNumber: 0
  }

  render() {
    const {number, fixedNumber} = this.state
    return (
      <div>
        <h1>{number}</h1>
        <h1>{fixedNumber}</h1>
        <button
          onClick={() => {
            this.setState(prevState => {
              return {
                number: prevState.number + 1
              }
            })
            // 아래 코드도 동일하게 동작
            // this.setState(prevState => ({
            //   number: prevState.number + 1
            // }))
          }}
        >
          +1
        </button>
      </div>
    )
  }
}

export default Counter;