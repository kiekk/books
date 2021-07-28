import { useState } from 'react'

const IterationSample = () => {
    const [names, setNames] = useState([
        { id: 1, text: '눈사람' },
        { id: 2, text: '얼음' },
        { id: 3, text: '눈' },
        { id: 4, text: '바람' }
    ])
    const [inputText, setInputText] = useState('')
    const [nextId, setNextId] = useState(5) // 새로운 항목을 추가할 때 사용할 ID

    const onChange = e => setInputText(e.target.value)
    const onClick = () => {
        const nextNames = names.concat({
            id: nextId,
            text: inputText
        })
        setNextId(nextId + 1)   // nextId에 1을 더한다
        setNames(nextNames) // names 값을 업데이트한다
        setInputText('')    // inputText를 비운다
    }

    const nameList = names.map(name => <li key={name.id}>{name.text}</li>)
    return (
        <>
            <input
                type="text"
                value={inputText}
                onChange={onChange}
            />
            <button onClick={onClick}>추가</button>
            <ul>
                {nameList}
            </ul>
        </>
    )
}

export default IterationSample