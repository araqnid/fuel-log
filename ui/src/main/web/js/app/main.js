import React from "react";
import ReactDOM from "react-dom";
import Root from "app/Root";
import Bus from "app/Bus";
import identity from "app/stores/identity";
import purchases from "app/stores/purchases";

window.BUS = new Bus();
identity.start();
purchases.start();

ReactDOM.render(React.createElement(Root), document.getElementById("component.Root"));
