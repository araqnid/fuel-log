import React from "react";
import {identity, preferences, purchases} from "./stores";
import NewFuelPurchaseEntry from "./NewFuelPurchaseEntry";
import FuelPurchaseList from "./FuelPurchaseList";
import Facade from "./Facade";

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
        identity.localUserIdentity.subscribe(this, user => {
            this.setState({ user: user });
        });
        purchases.purchaseList.subscribe(this, purchases => {
            this.setState({ purchases: purchases });
        });
        preferences.preferences.subscribe(this, preferences => {
            this.setState({ preferences: preferences });
        });
    }
    componentWillUnmount() {
        identity.unsubscribeAll(this);
        purchases.unsubscribeAll(this);
        preferences.unsubscribeAll(this);
    }
}
