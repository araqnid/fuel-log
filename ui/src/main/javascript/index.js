import "@babel/polyfill";
import React from "react";
import ReactDOM from "react-dom";
import Root, {IdentityStoreContext} from "./Root";
import "bootstrap";
import "bootstrap/dist/css/bootstrap.min.css";
import "./styles.css";
import IdentityStore from "./identity/IdentityStore";

const componentRootElt = document.createElement("div");
document.body.appendChild(componentRootElt);

const identityStore = new IdentityStore();

function render(RootComponent) {
    ReactDOM.render(<IdentityStoreContext.Provider value={identityStore}><RootComponent /></IdentityStoreContext.Provider>, componentRootElt);
}

if (process.env.NODE_ENV !== "production" && module.hot) {
    module.hot.accept('./Root.js', () => {
        render(require("./Root.js")["default"]);
    });
}

identityStore.start();

render(Root);
