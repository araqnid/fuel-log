import React from "react";
import ReactDOM from "react-dom";
import Root from "app/Root";
import Bus from "app/Bus";
import Identity from "app/Identity";
import identity from "app/stores/identity";
import purchases from "app/stores/purchases";
import Bootstrap from "bootstrap";
import styles from "../../../css/styles.css";

window.BUS = new Bus();
ReactDOM.render(<Root/>, document.getElementById("component.Root"));
ReactDOM.render(<Identity/>, document.getElementById("component.Identity"));
identity.start();
purchases.start();

console.log("for side-effect", Bootstrap, styles);
