import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import {createStore, applyMiddleware} from "redux";
import rootReducer from "./modules";
import {Provider} from 'react-redux'
import { createLogger} from "redux-logger/src";

const logger = createLogger();
const store = createStore(rootReducer, applyMiddleware(logger))

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <Provider store={store}>
    <App />
  </Provider>
);