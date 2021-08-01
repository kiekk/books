import React from 'react'
import ColorContext from '../contexts/color'

const ColorBox = () => {
    return (
        // props가 아닌 Consumer 컴포넌트를 통해 조회
        // Consumer 사이에 중괄호를 열어 함수를 넣어줌
        // Function as a child, Render Props
        // 컴포넌트의 child 자리에 JSX, 함수 전달
        <ColorContext.Consumer>
            {value => (
                <div
                    style={{
                        width: '64px',
                        height: '64px',
                        background: value.color
                    }}
                />
            )}
        </ColorContext.Consumer>
    )
}

export default ColorBox