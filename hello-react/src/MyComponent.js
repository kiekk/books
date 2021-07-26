const MyComponent = props => {
    return (
        <div>
            My First Component!<br />
            My Name is {props.name}<br />
            children은 {props.children}입니다.
        </div>
    )
}

MyComponent.defaultProps = {
    name: '김수박'
}

export default MyComponent;