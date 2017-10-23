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

(function(fontFamily, fallback) {
    document.head.appendChild((function() {
        const linkElt = document.createElement("link");
        linkElt.setAttribute("href", "https://fonts.googleapis.com/css?family=" + encodeURIComponent(fontFamily));
        linkElt.setAttribute("rel", "stylesheet");
        return linkElt;
    })());
    componentRootElt.style.cssText = "font-family: " + fontFamily + ", " + fallback;
})("Roboto", "sans-serif");

ReactDOM.render(React.createElement(Root), componentRootElt);
