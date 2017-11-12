import React from "react";
import {combineReducers} from "redux";
import {connect} from "react-redux";
import {bindActionPayload, resetOn} from "./util/Stores";
import {purchases} from "./stores";

const currencies = { 'GBP': { symbol: '£', places: 2 } };
const volumeUnits = { LITRES: "l", GALLONS: "gal" };
const distanceUnits = { MILES: "miles", KM: "km" };
const apiKey = "AIzaSyBcWvaL2aftj6o1PK3Jq5Hqm2lgUoh6amk";

function formatVolumeUnit(key) {
    return volumeUnits[key] || key + "?";
}

function formatDistanceUnit(key) {
    return distanceUnits[key] || key + "?";
}

export const reducer = combineReducers({
    attributes: resetOn("NewFuelPurchaseEntry/_reset")(combineReducers({
        fuelVolume: bindActionPayload("NewFuelPurchaseEntry/fuelVolume", ""),
        odometer: bindActionPayload("NewFuelPurchaseEntry/odometer", ""),
        location: bindActionPayload("NewFuelPurchaseEntry/location", ""),
        cost: bindActionPayload("NewFuelPurchaseEntry/cost", ""),
        fullFill: bindActionPayload("NewFuelPurchaseEntry/fullFill", false)
    })),
    geoLocation: bindActionPayload("NewFuelPurchaseEntry/geolocation", null),
    registering: resetOn("NewFuelPurchaseEntry/_reset")(bindActionPayload("NewFuelPurchaseEntry/_registering", false))
});

const GeoLocation = ({latitude, longitude}) => (
    <div>Geo-location available:
        <img src={"https://maps.googleapis.com/maps/api/staticmap?center=" + latitude + "," + longitude + "&zoom=13&size=300x300&sensor=false&key=" + apiKey} />
    </div>
);

class NewFuelPurchaseEntry extends React.Component {
    constructor() {
        super();
        this._onSubmit = this.onSubmit.bind(this);
        this._onInputChange = this.onInputChange.bind(this);
        this._geolocationWatchId = null;
    }

    render() {
        const { preferences: { currency, fuel_volume_unit: fuelVolumeUnit, distance_unit: distanceUnit }, newPurchase, geoLocation, registering } = this.props;

        const currencyDefinition = currencies[currency] || { symbol: currency, places: 2 };
        const costLabel = "Cost (" + currencyDefinition.symbol + ")";
        const fuelVolumeLabel = "Fuel volume (" + formatVolumeUnit(fuelVolumeUnit) + ")";
        const odoLabel = "Odometer reading (" + formatDistanceUnit(distanceUnit) + ")";

        return <div className="col-sm-4">
            <h2>Enter purchase</h2>
            <form onSubmit={ this._onSubmit }>
                <div className="form-group">
                    <label htmlFor="inputFuelVolume">{fuelVolumeLabel}</label>
                    <input type="number" onChange={this._onInputChange} className="form-control"
                           name="fuelVolume" id="inputFuelVolume" value={newPurchase.fuelVolume} placeholder="45.79" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputCost">{costLabel}</label>
                    <input type="number" onChange={this._onInputChange} className="form-control"
                           name="cost" id="inputCost" value={newPurchase.cost} placeholder="68.80" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputOdometer">{odoLabel}</label>
                    <input type="number" onChange={this._onInputChange} className="form-control"
                           name="odometer" id="inputOdometer" value={newPurchase.odometer} placeholder="111000" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputFullFill">Filled tank to full?</label>
                    <div className="form-check">
                        <label className="form-check-label">
                            <input type="checkbox" onChange={this._onInputChange} className="form-check-input"
                                   name="fullFill" id="inputFullFill" checked={newPurchase.fullFill ? "checked" : ""} />
                        </label>
                    </div>
                </div>

                <div className="form-group">
                    <label htmlFor="inputLocation">Location</label>
                    <input type="text" onChange={this._onInputChange} className="form-control"
                           name="location" id="inputLocation" value={newPurchase.location} placeholder="Tesco Elmers End" />
                </div>

                <button type="submit" className="btn btn-primary" disabled={registering}>Submit</button>

                {geoLocation ? <GeoLocation latitude={geoLocation.latitude} longitude={geoLocation.longitude} /> : null}
            </form>
        </div>;
    }
    componentDidMount() {
        purchases.subscribe(this, 'purchaseSubmitted', (fuelPurchaseId) => {
            this.props.dispatch({ type: "NewFuelPurchaseEntry/purchaseSubmitted", payload: fuelPurchaseId });
            this.props.dispatch({ type: "NewFuelPurchaseEntry/_reset" });
        });
        purchases.subscribe(this, 'purchaseSubmissionFailed', ({status, exception}) => {
            this.props.dispatch({ type: "NewFuelPurchaseEntry/purchaseSubmitted", payload: exception, error: true });
            this.props.dispatch({ type: "NewFuelPurchaseEntry/_registering", payload: false });
        });
        if (navigator.geolocation) {
            const options = {
                maximumAge: 60000
            };
            this._geolocationWatchId = navigator.geolocation.watchPosition(this.onGeolocationResult.bind(this), this.onGeolocationError.bind(this), options);
        }
    }
    componentWillUnmount() {
        purchases.unsubscribeAll(this);
        if (this._geolocationWatchId) {
            navigator.geolocation.clearWatch(this._geolocationWatchId);
        }
    }
    onInputChange(e) {
        const target = e.target;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const name = target.name;
        const newPurchase = this.props.newPurchase;
        if (newPurchase[name] !== undefined) {
            this.props.dispatch({ type: "NewFuelPurchaseEntry/" + name, payload: value });
        }
        else {
            console.warn("input change for unknown input", name, value, target);
        }
    }
    onSubmit(e) {
        e.preventDefault();
        this.props.dispatch({ type: "NewFuelPurchaseEntry/_registering", payload: true });
        const distanceFactor = this.props.preferences.distance_unit === "MILES" ? 1.60934 : 1;
        const fuelVolumeFactor = this.props.preferences.fuel_volume_unit === "GALLONS" ? 4.54609 : 1;
        const newPurchase = this.props.newPurchase;
        purchases.submit({
            odometer: newPurchase.odometer * distanceFactor,
            cost: {
                currency: this.props.preferences.currency,
                amount: newPurchase.cost
            },
            fuel_volume: newPurchase.fuelVolume * fuelVolumeFactor,
            full_fill: newPurchase.fullFill,
            location: newPurchase.location,
            geo_location: this.props.geoLocation
        });
    }
    onGeolocationResult(position) {
        this.props.dispatch({ type: "NewFuelPurchaseEntry/geolocation", payload: { latitude: position.coords.latitude, longitude: position.coords.longitude } });
    }
    onGeolocationError(error) {
        this.props.dispatch({ type: "NewFuelPurchaseEntry/geolocation", payload: error, error: true });
    }
}

export default connect(
    ({ preferences: { preferences }, newPurchase: { attributes: newPurchase, geoLocation, registering } }) => ({ preferences, newPurchase, geoLocation, registering })
)(NewFuelPurchaseEntry);
