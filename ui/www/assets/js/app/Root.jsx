define(['react', 'app/stores/identity', 'app/stores/purchases',
        'app/NewFuelPurchaseEntry', 'app/FuelPurchaseList', 'app/Facade'],
function(React, identity, purchases,
         NewFuelPurchaseEntry, FuelPurchaseList, Facade) {
    class Root extends React.Component {
        constructor() {
            super();
            this.state = { user: null, purchases: null };
        }
        render() {
            if (!this.state.user)
                return <div><Facade /></div>;
            return <div className="container">
                <div className="row">
                    <NewFuelPurchaseEntry />
                    <FuelPurchaseList purchases={this.state.purchases} />
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
        }
        componentWillUnmount() {
            identity.unsubscribe(this);
            purchases.unsubscribe(this);
        }
    }
    return Root;
});
