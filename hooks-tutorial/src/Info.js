import { useState } from 'react'
/*
    useState 함수는 하나의 상태 값만 관리,
    관리해야 할 상태 값이 여러 개라면 useState를 여러 번 사용
 */
const Info = () => {
    const [name, setName] = useState('')
    const [nickname, setNickname] = useState('')

    const onChangeName = e => {
        setName(e.target.value)
    }

    const onChangeNickname = e => {
        setNickname(e.target.value)
    }

    return (
        <div>
            <div>
                <input type="text" value={name} onChange={onChangeName}/>
                <input type="text" value={nickname} onChange={onChangeNickname}/>
            </div>
            <div>
                <div>
                    <b>이름:</b> {name}
                </div>
                <div>
                    <b>닉네임:</b> {nickname}
                </div>
            </div>
        </div>
    )
}

export default Info