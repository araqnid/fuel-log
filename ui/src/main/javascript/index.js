import "@babel/polyfill";
import _ from "lodash";
import React from "react";
import ReactDOM from "react-dom";
import {applyMiddleware, createStore} from "redux";
import {Provider} from "react-redux";
import Root from "./Root";
import reducers from "./reducers";
import storeFactory from "./stores";
import {reduxThunkWithStores} from "./util/Stores";
import "bootstrap";
import "bootstrap/dist/css/bootstrap.min.css";
import "./styles.css";

let stores = null;

const reduxDevToolsExtension = window.__REDUX_DEVTOOLS_EXTENSION__ ? window.__REDUX_DEVTOOLS_EXTENSION__() : x => x;

const redux = createStore(reducers, _.flow(reduxDevToolsExtension, applyMiddleware(reduxThunkWithStores(() => stores))));

if (process.env.NODE_ENV !== "production") {
    window.REDUX = redux;
    console.log("Redux store available as REDUX");
}

stores = storeFactory(redux);

Object.values(stores).forEach(store => {
    if (process.env.NODE_ENV !== "production") console.log("starting store", store);
    store.start();
});

if (process.env.NODE_ENV !== "production") {
    console.log("stores available as STORES");
    window.STORES = stores;
}

const componentRootElt = document.createElement("div");
document.body.appendChild(componentRootElt);

let RootComponent = Root;

function render() {
    ReactDOM.render(<Provider store={redux}><RootComponent /></Provider>, componentRootElt);
}

if (process.env.NODE_ENV !== "production" && module.hot) {
    module.hot.accept('./Root.js', () => {
        RootComponent = require("./Root.js")["default"];
        render();
    });
    module.hot.accept('./reducers.js', () => {
        const updatedReducers = require("./reducers.js")["default"];
        redux.replaceReducer(updatedReducers);
    });
    module.hot.accept('./stores.js', () => {
        const updatedStoresFactory = require("./stores.js")["default"];
        Object.values(stores).forEach(store => {
            console.log("stopping existing store", store);
            store.stop();
        });
        stores = updatedStoresFactory(redux);
        Object.values(stores).forEach(store => {
            console.log("starting replacement store", store);
            store.start();
        });
    });
}

render();
