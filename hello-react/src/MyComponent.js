import PropTypes from 'prop-types';

const MyComponent = ({ name, children }) => {
  return (
    <div>
      My First Component, prop: {name}<br/>
      children name is {children}
    </div>
  );
}

MyComponent.defaultProps = {
  name: 'Default Props Name, React'
}

MyComponent.propTypes = {
  name: PropTypes.string
}

export default MyComponent;