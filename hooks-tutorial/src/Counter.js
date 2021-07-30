import { useState } from 'react'
/*
    useState
    가장 기본적인 Hook, 함수형 컴포넌트에서 가변 값을 가질 수 있게 해줌
 */
const Counter = () => {
    const [value, setValue] = useState(0)

    return (
        <div>
            <p>현재 카운터 값은 <b>{value}</b>입니다.</p>
            <button onClick={() => setValue(value + 1)}>+1</button>
            <button onClick={() => setValue(value - 1)}>-1</button>
        </div>
    )
}

export default Counter