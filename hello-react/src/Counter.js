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
                        // prevState: 기존 상태
                        // props: 현재 지니고 있는 props
                        this.setState(prevState => {
                            return {
                                number: prevState.number + 1
                            }
                        })

                        // 동일하게 동작
                        // 아래 코드는 함수에서 바로 객체를 반환
                        this.setState(prevState => ({
                            number: prevState.number + 1
                        }))
                    }}
                >
                    +1
                </button>
            </div>
        )
    }
}

export default Counter