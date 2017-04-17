import React from "react";
import ReactDOM from "react-dom";
import Root from "app/Root";
import BUS from "app/message-bus";
import _ from "lodash";
import * as stores from "app/stores";

window.BUS = BUS;

_.forEach(stores, store => {
    if (process.env.NODE_ENV !== "production") console.log("starting store", store);
    store.start();
});

ReactDOM.render(React.createElement(Root), document.getElementById("component.Root"));
