import React from "react";
import Content from "./Content";
import Identity from "./Identity";

const Root = ({}) => (
    <div>
        <div className="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div className="container">
                <div className="navbar-header">
                    <button type="button" className="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                        <span className="sr-only">Toggle navigation</span>
                        <span className="icon-bar"/>
                        <span className="icon-bar"/>
                        <span className="icon-bar"/>
                    </button>
                    <a className="navbar-brand" id="fuellog-home-link" href="#">Fuel Log</a>
                </div>
                <div className="navbar-collapse collapse">
                    <Identity />
                </div>
            </div>
        </div>
        <Content />
    </div>
);

export default Root;
