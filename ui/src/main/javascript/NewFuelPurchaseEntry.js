import React, {useCallback, useEffect, useState} from "react";
import {useGeoLocation} from "./util/GeoLocator";
import * as ajax from "./util/Ajax";
import GeoLocationMap from "./GeoLocationMap";

const currencies = {'GBP': {symbol: '£', places: 2}};
const volumeUnits = {LITRES: "l", GALLONS: "gal"};
const distanceUnits = {MILES: "miles", KM: "km"};

function formatVolumeUnit(key) {
    return volumeUnits[key] || key + "?";
}

function formatDistanceUnit(key) {
    return distanceUnits[key] || key + "?";
}

function changeTextState(setter) {
    return e => {
        e.preventDefault();
        setter(e.target.value);
    }
}

function changeCheckboxState(setter) {
    return e => {
        e.preventDefault();
        setter(!!e.target.checked);
    }
}

const NewFuelPurchaseEntry = ({preferences: {currency, fuelVolumeUnit, distanceUnit}, onPurchaseSaved}) => {
    const currencyDefinition = currencies[currency] || {symbol: currency, places: 2};
    const costLabel = `Cost (${currencyDefinition.symbol})`;
    const fuelVolumeLabel = `Fuel volume (${formatVolumeUnit(fuelVolumeUnit)})`;
    const odoLabel = `Odometer reading (${formatDistanceUnit(distanceUnit)})`;

    const [fuelVolume, setFuelVolume] = useState("");
    const [odometer, setOdometer] = useState("");
    const [location, setLocation] = useState("");
    const [cost, setCost] = useState("");
    const [fullFill, setFullFill] = useState(false);

    const [registering, setRegistering] = useState(false);

    const geoLocation = useGeoLocation();

    const onSubmit = useCallback(e => {
        e.preventDefault();
        setRegistering(true);
    }, []);

    useEffect(() => {
        if (!registering) return;
        const distanceFactor = distanceUnit === "MILES" ? 1.60934 : 1;
        const fuelVolumeFactor = fuelVolumeUnit === "GALLONS" ? 4.54609 : 1;

        const newPurchase = {
            odometer: odometer * distanceFactor,
            cost: {
                currency: currency,
                amount: cost
            },
            fuel_volume: fuelVolume * fuelVolumeFactor,
            full_fill: fullFill,
            location: location,
            geo_location: geoLocation
        };
        console.log("save purchase", newPurchase);
        const subscription = ajax.postRaw("fuel", newPurchase).subscribe(
            ({ status, headers }) => {
                if (status !== 201) {
                    console.error(`status was ${status}`);
                    return;
                }
                const location = headers.location;
                const matches = location.match(/\/_api\/fuel\/(.+)$/);
                if (!matches) {
                    console.warn("invalid creation URI", location);
                    return;
                }
                const fuelPurchaseId = matches[1];
                console.log("saved purchase ", fuelPurchaseId);
                setFuelVolume("");
                setOdometer("");
                setLocation("");
                setCost("");
                setFullFill(false);
                onPurchaseSaved(fuelPurchaseId);
            }
        );
        return () => {
            subscription.unsubscribe();
        }
    }, [registering]);

    const onFuelVolumeChange = useCallback(changeTextState(setFuelVolume), []);
    const onOdometerChange = useCallback(changeTextState(setOdometer), []);
    const onLocationChange = useCallback(changeTextState(setLocation), []);
    const onCostChange = useCallback(changeTextState(setCost), []);
    const onFullFillChange = useCallback(changeCheckboxState(setFullFill), []);

    return (
        <div className="col-sm-4">
            <h2>Enter purchase</h2>
            <form onSubmit={onSubmit}>
                <div className="form-group">
                    <label htmlFor="inputFuelVolume">{fuelVolumeLabel}</label>
                    <input type="number" onChange={onFuelVolumeChange} className="form-control"
                           name="fuelVolume" id="inputFuelVolume" value={fuelVolume}
                           placeholder="45.79"/>
                </div>

                <div className="form-group">
                    <label htmlFor="inputCost">{costLabel}</label>
                    <input type="number" onChange={onCostChange} className="form-control"
                           name="cost" id="inputCost" value={cost} placeholder="68.80"/>
                </div>

                <div className="form-group">
                    <label htmlFor="inputOdometer">{odoLabel}</label>
                    <input type="number" onChange={onOdometerChange} className="form-control"
                           name="odometer" id="inputOdometer" value={odometer} placeholder="111000"/>
                </div>

                <div className="form-group">
                    <label htmlFor="inputFullFill">Filled tank to full?</label>
                    <div className="form-check">
                        <label className="form-check-label">
                            <input type="checkbox" onChange={onFullFillChange} className="form-check-input"
                                   name="fullFill" id="inputFullFill"
                                   checked={fullFill ? "checked" : ""}/>
                        </label>
                    </div>
                </div>

                <div className="form-group">
                    <label htmlFor="inputLocation">Location</label>
                    <input type="text" onChange={onLocationChange} className="form-control"
                           name="location" id="inputLocation" value={location}
                           placeholder="Tesco Elmers End"/>
                </div>

                <button type="submit" className="btn btn-primary" disabled={registering}>Submit</button>

                {geoLocation ?
                    <GeoLocationMap latitude={geoLocation.latitude} longitude={geoLocation.longitude}/> : null}
            </form>
        </div>
    );
};

export default NewFuelPurchaseEntry;
