import PropTypes from 'prop-types';

const MyComponent = ({ name, favoriteNumber, children }) => {
  return (
    <div>
      My First Component, prop: {name}<br/>
      children name is {children}
      <br />
      My FavoriteNumber is {favoriteNumber}
    </div>
  );
}

MyComponent.defaultProps = {
  name: 'Default Props Name, React'
}

MyComponent.propTypes = {
  name: PropTypes.string,
  favoriteNumber: PropTypes.number.isRequired
}

export default MyComponent;