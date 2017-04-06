define(['react'],
function(React) {
    class NewFuelPurchaseEntry extends React.Component {
        constructor() {
            super();
            this._onSubmit = this.onSubmit.bind(this);
        }
        render() {
            return <div>
                <div className="container">
                    <div className="row">
                        <p>Enter fuel purchase</p>
                        <form onSubmit={ this._onSubmit }>
                            <div className="form-group">
                                <label htmlFor="inputFuelVolume">Fuel volume (litres)</label>
                                <input type="text" className="form-control" id="inputFuelVolume" placeholder="45.79" />
                            </div>

                            <div className="form-group">
                                <label htmlFor="inputCost">Cost (£)</label>
                                <input type="text" className="form-control" id="inputCost" placeholder="68.80" />
                            </div>

                            <div className="form-group">
                                <label htmlFor="inputOdometer">Odometer reading (miles)</label>
                                <input type="text" className="form-control" id="inputOdometer" placeholder="111000" />
                            </div>

                            <div className="form-group">
                                <label htmlFor="inputFullFill">Filled tank to full?</label>
                                <div className="form-check">
                                    <label className="form-check-label">
                                        <input type="checkbox" className="form-check-input" id="inputFullFill" />
                                    </label>
                                </div>
                            </div>

                            <div className="form-group">
                                <label htmlFor="inputLocation">Location</label>
                                <input type="text" className="form-control" id="inputLocation" placeholder="Tesco Elmers End" />
                            </div>

                            <button type="submit" className="btn btn-primary">Submit</button>
                        </form>
                    </div>
                </div>
            </div>;
        }
        onSubmit(e) {
            e.preventDefault();
            $.ajax({
                headers: { accept: 'application/json' },
                url: "/_api/fuel",
                method: "POST",
                contentType: 'application/json',
                data: JSON.stringify({
                    odometer: 0,
                    cost: {
                        currency: 'GBP',
                        amount: 0.0
                    },
                    fuel_volume: 0.0,
                    full_fill: false,
                    location: ''
                }),
                success: (data, status, xhr) => {
                    console.log("fuel post success", data, status, xhr);
                },
                error: (xhr, status, ex) => {
                    console.log("fuel post failed", e, status, xhr);
                },
                complete: (xhr, status) => {
                    this.registering = null;
                }
            });
        }
    }
    return NewFuelPurchaseEntry;
});
