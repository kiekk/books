import { useState } from 'react'

const Say = () => {
    // useState의 첫 번째 원소: 현재 상태
    // 두 번째 원소: 상태를 바꿔주는 함수(setter)
    const [ message, setMessage ] = useState('')
    const onClickEnter = () => setMessage('Hello!')
    const onClickLeave = () => setMessage('GoodBye!')

    const [color, setColor] = useState('')
    
    return (
        <div>
            <button onClick={onClickEnter}>입장</button>
            <button onClick={onClickLeave}>퇴장</button>
            <h1 style={{color}}>{message}</h1>
            <button style={{color: 'red'}} onClick={() => setColor('red')}>빨간색</button>
            <button style={{color: 'green'}} onClick={() => setColor('green')}>초록색</button>
            <button style={{color: 'blue'}} onClick={() => setColor('blue')}>파랑색</button>
        </div>
    )
}

export default Say