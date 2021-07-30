import { useState, useMemo, useCallback } from 'react'

/*
    기존 Average 컴포넌트의 onChange, onInsert는 
    컴포넌트가 리렌더링 될 때마다 새로 만들어진 함수를 사용.
    
    useCallback은 만들어 놨던 함수 재사용 가능
 */
const getAverage = numbers => {
    console.log('평균값 계산 중...')
    if (numbers.length === 0) {
        return 0
    }
    const sum = numbers.reduce((a, b) => a + b)
    return sum / numbers.length
}

const Average = () => {
    const [list, setList] = useState([])
    const [number, setNumber] = useState('')

    const onChange = useCallback(e => {
        setNumber(e.targat.value)
    }, [])  // 컴포넌트가 처음 렌더링 될 때만 함수 생성

    /*
        useCallback 첫 번째 파라미터: 생성하고 싶은 함수
        두 번째 파라미터: 배열(어떤 값이 바뀌었을 때 함수를 새로 생성해야 하는지 명시)
        
        onChange처럼 [] 빈 배열인 경우 컴포넌트 렌더링시 만들었던 함수를 재사용
        onInsert처럼 number, list를 넣게 되면 number, list가 바뀔 때 새로 만들어진 함수 사용
     */
    const onInsert = useCallback(() => {
        const nextList = list.concat(parseInt(number))
        setList(nextList)
        setNumber('')
    }, [number, list])  // number, list가 바뀌었을 때만 함수 생성

    const avg = useMemo(() => getAverage(list), [list])

    return (
        <div>
            <input type="text" value={number} onChange={onChange} />
            <button onClick={onInsert}>등록</button>
            <ul>
                {list.map((value, index) => (
                    <li key={index}>{value}</li>
                ))}
            </ul>
            <div>
                <b>평균값:</b> {avg}
            </div>
        </div>
    )
}

export default Average