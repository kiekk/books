import { Component } from 'react'

class Counter extends Component {
    state = {
        number: 0,
        fixedNumber: 0,
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
                        this.setState(
                            {
                                number: number + 1
                            },
                            () => {
                                console.log('setState 호출')
                                console.log(this.state)
                            }
                        )
                    }}
                >
                    +1
                </button>
            </div>
        )
    }
}

export default Counter