import React from "react";
import ReactDOM from "react-dom";
import {createStore} from "redux";
import {Provider} from "react-redux";
import Root from "./Root";
import reducers from "./reducers";
import * as stores from "./stores";
import {actions as identityActions} from "./stores/IdentityStore";

const redux = createStore(reducers, window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__());

if (process.env.NODE_ENV !== "production") {
    window.REDUX = redux;
    console.log("Redux store available as REDUX");
}

Object.values(stores).forEach(store => {
    if (process.env.NODE_ENV !== "production") console.log("starting store", store);
    store.begin();
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

ReactDOM.render(<Provider store={redux}><Root /></Provider>, componentRootElt);

identityActions(redux.dispatch).begin();
