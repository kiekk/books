import React, {useContext} from 'react'
import ColorContext from '../contexts/color'

const ColorBox = () => {
    const {state} = useContext(ColorContext)

    return (
        <>
            <div
                style={{
                    width: '64px',
                    height: '64px',
                    background: state.color
                }}
            />
            <div
                style={{
                    width: '64px',
                    height: '64px',
                    background: state.subColor
                }}
            />
        </>
    )
}

export default ColorBox