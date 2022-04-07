import {Component} from 'react'

class Counter extends Component {
  constructor(props) {
    super(props);
    this.state = {
      number: 0
    }
  }

  render() {
    const {number} = this.state
    return (
      <div>
        <h1>{number}</h1>
        <button
          onClick={() => {
            this.setState({number: number + 1})
          }}
        >
          +1
        </button>
      </div>
    )
  }
}

export default Counter;