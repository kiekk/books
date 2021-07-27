import { useState } from 'react'

const Say = () => {
    // useState의 첫 번째 원소: 현재 상태
    // 두 번째 원소: 상태를 바꿔주는 함수(setter)
    const [ message, setMessage ] = useState('')
    const onClickEnter = () => setMessage('Hello!')
    const onClickLeave = () => setMessage('GoodBye!')
    
    return (
        <div>
            <button onClick={onClickEnter}>입장</button>
            <button onClick={onClickLeave}>퇴장</button>
            <h1>{message}</h1>
        </div>
    )
}

export default Say