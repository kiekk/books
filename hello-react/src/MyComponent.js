const MyComponent = props => {
  return <div>My First Component, prop: {props.name}</div>
}

MyComponent.defaultProps = {
  name: 'Default Props Name, React'
}

export default MyComponent;