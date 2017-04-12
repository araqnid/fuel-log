import React from "react";

export default class NewFuelPurchaseEntry extends React.Component {
    constructor() {
        super();
        this._onSubmit = this.onSubmit.bind(this);
        this._onInputChange = this.onInputChange.bind(this);
        this._defaultState = { fuelVolume: "", odometer: "", location: "", cost: "", fullFill: false, registering: false };
        this.state = this._defaultState;
    }
    render() {
        return <div className="col-sm-4">
            <h2>Enter purchase</h2>
            <form onSubmit={ this._onSubmit }>
                <div className="form-group">
                    <label htmlFor="inputFuelVolume">Fuel volume (litres)</label>
                    <input type="text" onChange={this._onInputChange} className="form-control"
                           name="fuelVolume" id="inputFuelVolume" value={this.state.fuelVolume} placeholder="45.79" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputCost">Cost (£)</label>
                    <input type="text" onChange={this._onInputChange} className="form-control"
                           name="cost" id="inputCost" value={this.state.cost} placeholder="68.80" />
                </div>

                <div className="form-group">
                    <label htmlFor="inputOdometer">Odometer reading (miles)</label>
                    <input type="text" onChange={this._onInputChange} className="form-control"
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
        $.ajax({
            headers: { accept: 'application/json' },
            url: "/_api/fuel",
            method: "POST",
            contentType: 'application/json',
            data: JSON.stringify({
                odometer: this.state.odometer * 1.60934,
                cost: {
                    currency: 'GBP',
                    amount: this.state.cost
                },
                fuel_volume: this.state.fuelVolume,
                full_fill: this.state.fullFill,
                location: this.state.location
            }),
            success: (data, status, xhr) => {
                this.setState(this._defaultState);
                if (xhr.status !== 201) {
                    BUS.broadcast("NewFuelPurchaseEntry.PurchaseSubmissionFailed", {status: xhr.status, exception: null, data: data});
                    return;
                }
                const location = xhr.getResponseHeader('Location');
                const matches = location.match(/\/_api\/fuel\/(.+)$/);
                if (!matches) {
                    console.warn("invalid creation URI", location);
                    return;
                }
                const fuelPurchaseId = matches[1];
                BUS.broadcast("NewFuelPurchaseEntry.PurchaseSubmitted", fuelPurchaseId);
            },
            error: (xhr, status, ex) => {
                BUS.broadcast("NewFuelPurchaseEntry.PurchaseSubmissionFailed", {status: status, exception: ex});
            },
            complete: (xhr, status) => {
                this.setState({ registering: false });
            }
        });
    }
}
