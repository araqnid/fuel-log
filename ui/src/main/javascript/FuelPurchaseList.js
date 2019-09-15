import React, {useEffect, useState} from "react";
import {Instant, ZonedDateTime, ZoneId} from "js-joda";
import * as ajax from "./util/Ajax";
import autoRefresh from "./util/autoRefresh";

function formatDistance(value, unit) {
    const convertedValue = unit === "MILES" ? value / 1.60934 : value;
    return convertedValue.toFixed(0);
}

function formatFuelVolume(value, unit) {
    const convertedValue = unit === "GALLONS" ? value / 4.54609 : value;
    return convertedValue.toFixed(2);
}

const currencies = {'GBP': {symbol: '£', places: 2}};

function formatCost(value) {
    const definition = currencies[value.currency] || {symbol: value.currency + " ", places: 2};
    return definition.symbol + value.amount.toFixed(definition.places);
}

function instantFromEpochSecondsDecimal(epochTime) {
    const epochSecond = Math.trunc(epochTime);
    const nanoAdjustment = Math.trunc((epochTime - epochSecond) * 1E9);
    return Instant.ofEpochSecond(epochSecond, nanoAdjustment);
}

const FuelPurchase = ({purchase, distanceUnit, fuelVolumeUnit}) => {
    const purchaseInstant = instantFromEpochSecondsDecimal(purchase.purchased_at);
    const purchaseDateTime = ZonedDateTime.ofInstant(purchaseInstant, ZoneId.SYSTEM);
    const purchaseDate = purchaseDateTime.toLocalDate();
    return <tr>
        <td>{purchaseDate.toString()}</td>
        <td>{formatDistance(purchase.odometer, distanceUnit)}</td>
        <td>{formatFuelVolume(purchase.fuel_volume, fuelVolumeUnit)}</td>
        <td>{purchase.full_fill ? "Yes" : ""}</td>
        <td>{formatCost(purchase.cost)}</td>
        <td>{purchase.location_string}</td>
    </tr>
};

const FuelPurchaseList = ({preferences: {distanceUnit, fuelVolumeUnit}, saveCounter}) => {
    const [purchases, setPurchases] = useState(null);
    useEffect(() => {
        const subscription = autoRefresh(60000)(ajax.get("fuel")).subscribe(
            payload => {
                payload.sort((a, b) => b.purchased_at - a.purchased_at);
                setPurchases(payload);
            }
        );
        return () => {
            subscription.unsubscribe();
        };
    }, [saveCounter]);

    if (!purchases) {
        return <div className="col-sm-8"/>;
    }

    const distanceLabel = distanceUnit === "MILES" ? "Miles" : "Km";
    const fuelVolumeLabel = fuelVolumeUnit === "GALLONS" ? "Gallons" : "Litres";

    return <div className="col-sm-8">
        <h2>Purchase log</h2>
        <table className="table">
            <thead>
            <tr>
                <td>Date</td>
                <td>{distanceLabel}</td>
                <td>{fuelVolumeLabel}</td>
                <td>Full fill?</td>
                <td>Cost</td>
                <td>Where?</td>
            </tr>
            </thead>
            <tbody>
            {purchases.map(purchase => (
                <FuelPurchase key={purchase.fuel_purchase_id} purchase={purchase} distanceUnit={distanceUnit} fuelVolumeUnit={fuelVolumeUnit}/>
            ))}
            </tbody>
        </table>
    </div>;
};

export default FuelPurchaseList;
