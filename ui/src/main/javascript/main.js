import React from "react";
import ReactDOM from "react-dom";
import Root from "./Root";
import _ from "lodash";
import * as stores from "./stores";

_.forEach(stores, store => {
    if (process.env.NODE_ENV !== "production") console.log("starting store", store);
    store.begin();
});

ReactDOM.render(React.createElement(Root), document.getElementById("component.Root"));
