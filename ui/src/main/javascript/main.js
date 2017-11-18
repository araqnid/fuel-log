import React from "react";
import ReactDOM from "react-dom";
import {createStore} from "redux";
import {Provider} from "react-redux";
import Root from "./Root";
import reducers from "./reducers";
import storeFactory from "./stores";

const redux = createStore(reducers, window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__());

if (process.env.NODE_ENV !== "production") {
    window.REDUX = redux;
    console.log("Redux store available as REDUX");
}

let stores = storeFactory(redux);

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

(function(fontFamily, fallback) {
    document.head.appendChild((function() {
        const linkElt = document.createElement("link");
        linkElt.setAttribute("href", "https://fonts.googleapis.com/css?family=" + encodeURIComponent(fontFamily));
        linkElt.setAttribute("rel", "stylesheet");
        return linkElt;
    })());
    componentRootElt.style.cssText = "font-family: " + fontFamily + ", " + fallback;
})("Roboto", "sans-serif");

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
