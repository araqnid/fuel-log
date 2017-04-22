import React from "react";
import {identity, preferences, purchases} from "app/stores";
import NewFuelPurchaseEntry from "app/NewFuelPurchaseEntry";
import FuelPurchaseList from "app/FuelPurchaseList";
import Facade from "app/Facade";

export default class Content extends React.Component {
    constructor() {
        super();
        this.state = { user: null, purchases: null, preferences: null };
    }
    render() {
        if (!this.state.user || !this.state.preferences)
            return <div><Facade /></div>;
        return <div className="container">
            <div className="row">
                <NewFuelPurchaseEntry preferences={this.state.preferences} />
                <FuelPurchaseList purchases={this.state.purchases} preferences={this.state.preferences} />
            </div>
        </div>;
    }
    componentDidMount() {
        identity.subscribe({
            localIdentity: user => {
                this.setState({ user: user });
            }
        }, this);
        purchases.subscribe({
            purchaseList: purchases => {
                this.setState({ purchases: purchases });
            }
        }, this);
        preferences.subscribe({
            preferences: preferences => {
                this.setState({ preferences: preferences });
            }
        }, this);
    }
    componentWillUnmount() {
        identity.unsubscribe(this);
        purchases.unsubscribe(this);
        preferences.unsubscribe(this);
    }
}
