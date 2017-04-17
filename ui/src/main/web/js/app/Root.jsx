import React from "react";
import Content from "app/Content";
import Identity from "app/Identity";

export default function Root() {
    return <div>
        <div className="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div className="container">
                <div className="navbar-header">
                    <button type="button" className="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                        <span className="sr-only">Toggle navigation</span>
                        <span className="icon-bar"></span>
                        <span className="icon-bar"></span>
                        <span className="icon-bar"></span>
                    </button>
                    <a className="navbar-brand" id="fuellog-home-link" href="#">Fuel Log</a>
                </div>
                <Identity />
            </div>
        </div>
        <Content />
    </div>;
}
