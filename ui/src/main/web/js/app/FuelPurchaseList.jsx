import React from "react";
import _ from "lodash";

export default props => {
    if (!props.purchases) {
        return <div className="col-sm-8" />;
    }

    const toRow = purchase => {
        const date = new Date(purchase.purchased_at * 1000);
        const money = (purchase.cost.currency === "GBP" ? "£" : purchase.cost.currency + " ") + purchase.cost.amount.toFixed(2);
        return <tr key={ purchase.fuel_purchase_id }>
            <td>{ date.toLocaleString() }</td>
            <td>{ (purchase.odometer / 1.60934).toFixed(0) }</td>
            <td>{ purchase.fuel_volume.toFixed(2) }</td>
            <td>{ purchase.full_fill ? "Yes" : "" }</td>
            <td>{ money }</td>
            <td>{ purchase.location_string }</td>
        </tr>
    };

    return <div className="col-sm-8">
        <h2>Purchase log</h2>
        <table className="table">
            <thead>
            <tr>
                <td>Date</td>
                <td>Odo</td>
                <td>Litres</td>
                <td>Full fill?</td>
                <td>Cost</td>
                <td>Where?</td>
            </tr>
            </thead>
            <tbody>
            { _(props.purchases).sortBy( p => p.purchased_at ).reverse().map(toRow).value() }
            </tbody>
        </table>
    </div>;
};
