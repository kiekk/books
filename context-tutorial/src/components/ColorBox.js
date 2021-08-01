import React from 'react'
import { ColorConsumer } from '../contexts/color'

const ColorBox = () => {
    return (
        <ColorConsumer>
            {value => (
                <>
                    <div
                        style={{
                            width: '64px',
                            height: '64px',
                            background: value.state.color
                        }}
                    />
                    <div
                        style={{
                            width: '64px',
                            height: '64px',
                            background: value.state.subColor
                        }}
                    />
                </>
            )}
        </ColorConsumer>
    )
}

export default ColorBox