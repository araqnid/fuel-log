import React from "react";
import _ from "lodash";
import {connect} from "react-redux";
import NewFuelPurchaseEntry from "./NewFuelPurchaseEntry";
import FuelPurchaseList from "./FuelPurchaseList";
import Facade from "./Facade";

const Content = ({user, preferences, purchases}) => {
    if (!user || !preferences) {
        return <div><Facade /></div>;
    }
    return <div className="container">
        <div className="row">
            <NewFuelPurchaseEntry preferences={preferences} />
            <FuelPurchaseList purchases={purchases} preferences={preferences} />
        </div>
    </div>;
};

export default connect(
    ({ identity: { localUserIdentity }, purchases: { purchaseList }, preferences: { preferences } }) => ({
        user: localUserIdentity,
        purchases: _(purchaseList).sortBy("purchased_at").reverse().value(),
        preferences
    }),
    (dispatch) => ({})
)(Content);
