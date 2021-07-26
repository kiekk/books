const MyComponent = props => {
    return (
        <div>
            My First Component!<br />
            My Name is {props.name}
        </div>
    )
}

MyComponent.defaultProps = {
    name: '김수박'
}

export default MyComponent;