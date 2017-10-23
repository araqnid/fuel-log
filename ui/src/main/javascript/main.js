import React from "react";
import ReactDOM from "react-dom";
import Root from "./Root";
import * as stores from "./stores";

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

ReactDOM.render(React.createElement(Root), componentRootElt);
