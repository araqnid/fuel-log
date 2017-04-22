import React from "react";
import BUS from "app/message-bus";
import {purchases} from "app/stores";

const currencies = { 'GBP': { symbol: '£', places: 2 } };
const volumeUnits = { LITRES: "l", GALLONS: "gal" };
const distanceUnits = { MILES: "miles", KM: "km" };

function formatVolumeUnit(key) {
    return volumeUnits[key] || key + "?";
}

function formatDistanceUnit(key) {
    return distanceUnits[key] || key + "?";
}

export default class NewFuelPurchaseEntry extends React.Component {
    constructor() {
        super();
        this._onSubmit = this.onSubmit.bind(this);
        this._onInputChange = this.onInputChange.bind(this);
        this._defaultState = { fuelVolume: "", odometer: "", location: "", cost: "", fullFill: false, registering: false };
        this.state = this._defaultState;
    }
    render() {
        const currencyDefinition = currencies[this.props.preferences.currency] || { symbol: this.props.preferences.currency, places: 2 };
        const costLabel = "Cost (" + currencyDefinition.symbol + ")";
        const fuelVolumeLabel = "Fuel volume (" + formatVolumeUnit(this.props.preferences.fuel_volume_unit) + ")";
        const odoLabel = "Odometer reading (" + formatDistanceUnit(this.props.preferences.distance_unit) + ")";

        return <div className="col-sm-4">
            <h2>Enter purchase</h2>
            <form onSubmit={ this._onSubmit }>
                <div className="form-group">
                    <label htmlFor="inputFuelVolume">{fuelVolumeLabel}</label>
                    <input type="number" onChange={this._onInputChange} className="form-control"
                           name="fuelVolume" id="inputFuelVolume" value={this.state.fuelVolume} placeholder="45.79" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputCost">{costLabel}</label>
                    <input type="number" onChange={this._onInputChange} className="form-control"
                           name="cost" id="inputCost" value={this.state.cost} placeholder="68.80" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputOdometer">{odoLabel}</label>
                    <input type="number" onChange={this._onInputChange} className="form-control"
                           name="odometer" id="inputOdometer" value={this.state.odometer} placeholder="111000" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputFullFill">Filled tank to full?</label>
                    <div className="form-check">
                        <label className="form-check-label">
                            <input type="checkbox" onChange={this._onInputChange} className="form-check-input"
                                   name="fullFill" id="inputFullFill" checked={this.state.fullFill ? "checked" : ""} />
                        </label>
                    </div>
                </div>

                <div className="form-group">
                    <label htmlFor="inputLocation">Location</label>
                    <input type="text" onChange={this._onInputChange} className="form-control"
                           name="location" id="inputLocation" value={this.state.location} placeholder="Tesco Elmers End" />
                </div>

                <button type="submit" className="btn btn-primary" disabled={this.state.registering}>Submit</button>
            </form>
        </div>;
    }
    componentDidMount() {
        BUS.subscribe("NewFuelPurchaseEntry.PurchaseSubmitted", (fuelPurchaseId) => {
            this.setState(this._defaultState);
        }, this);
        BUS.subscribe("NewFuelPurchaseEntry.PurchaseSubmissionFailed", (code, ex) => {
            this.setState({ registering: false });
        }, this);
    }
    componentWillUnmount() {
        BUS.unsubscribe(this);
    }
    onInputChange(e) {
        const target = e.target;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const name = target.name;
        if (this.state[name] !== undefined) {
            this.setState({ [name]: value });
        }
        else {
            console.warn("input change for unknown input", name, value, target);
        }
    }
    onSubmit(e) {
        e.preventDefault();
        this.setState({ registering: true });
        const distanceFactor = this.props.preferences.distance_unit === "MILES" ? 1.60934 : 1;
        const fuelVolumeFactor = this.props.preferences.fuel_volume_unit === "GALLONS" ? 4.54609 : 1;
        purchases.submit({
            odometer: this.state.odometer * distanceFactor,
            cost: {
                currency: this.props.preferences.currency,
                amount: this.state.cost
            },
            fuel_volume: this.state.fuelVolume * fuelVolumeFactor,
            full_fill: this.state.fullFill,
            location: this.state.location
        });
    }
}
