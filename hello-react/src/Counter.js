import { Component } from 'react'

class Counter extends Component {
    constructor(props) {
        super(props)
        // state의 초기값 설정
        this.state = {
            number: 0,
            fixedNumber: 0,
        }
    }
    render() {
        // this.state로 접근
        const { number, fixedNumber } = this.state
        return (
            <div>
                <h1>{number}</h1>
                <h2>바뀌지 않는 값 : {fixedNumber}</h2>
                <button
                    onClick={() => {
                        // this.setState로 state 변경
                        this.setState({ number: number + 1})
                    }}
                >
                    +1
                </button>
            </div>
        )
    }
}

export default Counter