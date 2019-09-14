import "@babel/polyfill";
import React from "react";
import ReactDOM from "react-dom";
import Root from "./Root";
import "bootstrap";
import "bootstrap/dist/css/bootstrap.min.css";
import "./styles.css";

const componentRootElt = document.createElement("div");
document.body.appendChild(componentRootElt);

function render(RootComponent) {
    ReactDOM.render(<RootComponent />, componentRootElt);
}

if (process.env.NODE_ENV !== "production" && module.hot) {
    module.hot.accept('./Root.js', () => {
        render(require("./Root.js")["default"]);
    });
}

render(Root);
