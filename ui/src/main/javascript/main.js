import React from "react";
import ReactDOM from "react-dom";
import Root from "./Root";
import BUS from "./message-bus";
import _ from "lodash";
import * as stores from "./stores";

window.BUS = BUS;

_.forEach(stores, store => {
    if (process.env.NODE_ENV !== "production") console.log("starting store", store);
    store.start();
});

ReactDOM.render(React.createElement(Root), document.getElementById("component.Root"));
