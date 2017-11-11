import React from "react";
import {connect} from "react-redux";
import NewFuelPurchaseEntry from "./NewFuelPurchaseEntry";
import FuelPurchaseList from "./FuelPurchaseList";
import Facade from "./Facade";

const Content = ({signedIn}) => {
    return !signedIn ? <div><Facade /></div>
            : <div className="container">
            <div className="row">
                <NewFuelPurchaseEntry />
                <FuelPurchaseList />
            </div>
        </div>;
};

export default connect(
    ({ identity: { localUserIdentity }, preferences: { preferences } }) => ({
        signedIn: localUserIdentity !== null && preferences !== null
    }),
    (dispatch) => ({})
)(Content);
