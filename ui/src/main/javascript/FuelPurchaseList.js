import React from "react";
import {connect} from "react-redux";
import {Instant, ZonedDateTime, ZoneId} from "js-joda";

function formatDistance(value, preferences) {
    const unit = preferences.distance_unit || "KM";
    const convertedValue = unit === "MILES" ? value / 1.60934 : value;
    return convertedValue.toFixed(0);
}

function formatFuelVolume(value, preferences) {
    const unit = preferences.fuel_unit || "LITRES";
    const convertedValue = unit === "GALLONS" ? value / 4.54609 : value;
    return convertedValue.toFixed(2);
}

const currencies = { 'GBP': { symbol: '£', places: 2 } };

function formatCost(value, preferences) {
    const definition = currencies[value.currency] || { symbol: value.currency + " ", places : 2 };
    return definition.symbol + value.amount.toFixed(definition.places);
}

const FuelPurchase = ({purchase, preferences}) => {
    const purchaseInstant = Instant.ofEpochMilli(purchase.purchased_at * 1000);
    const purchaseDateTime = ZonedDateTime.ofInstant(purchaseInstant, ZoneId.SYSTEM);
    const purchaseDate = purchaseDateTime.toLocalDate();
    return <tr>
        <td>{ purchaseDate.toString() }</td>
        <td>{ formatDistance(purchase.odometer, preferences) }</td>
        <td>{ formatFuelVolume(purchase.fuel_volume, preferences) }</td>
        <td>{ purchase.full_fill ? "Yes" : "" }</td>
        <td>{ formatCost(purchase.cost, preferences) }</td>
        <td>{ purchase.location_string }</td>
    </tr>
};

const FuelPurchaseList = ({purchases, preferences}) => {
    if (!purchases) {
        return <div className="col-sm-8" />;
    }

    const distanceUnit = preferences.distance_unit || "KM";
    const distanceLabel = distanceUnit === "MILES" ? "Miles" : "Km";

    const fuelVolumeUnit = preferences.fuel_unit || "LITRES";
    const fuelVolumeLabel = fuelVolumeUnit === "GALLONS" ? "Gallons" : "Litres";

    return <div className="col-sm-8">
        <h2>Purchase log</h2>
        <table className="table">
            <thead>
            <tr>
                <td>Date</td>
                <td>{ distanceLabel }</td>
                <td>{ fuelVolumeLabel }</td>
                <td>Full fill?</td>
                <td>Cost</td>
                <td>Where?</td>
            </tr>
            </thead>
            <tbody>
            { purchases.map(purchase => (
                <FuelPurchase key={ purchase.fuel_purchase_id } purchase={purchase} preferences={preferences} />
            )) }
            </tbody>
        </table>
    </div>;
};

export default connect(
    ({ purchases: { purchaseList }, preferences: { preferences } }) => ({
        purchases: _(purchaseList).sortBy("purchased_at").reverse().value(),
        preferences
    }),
    dispatch => ({})
)(FuelPurchaseList);
