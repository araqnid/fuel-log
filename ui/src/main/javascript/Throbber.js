import React from "react";

const Throbber = ({ children = "Loading..." }) => (
    <div className="spinner-border" role="status">
        <span className="sr-only">{children}</span>
    </div>
);

export default Throbber;
