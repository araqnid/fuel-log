define(['react', 'app/stores/identity',
        'app/NewFuelPurchaseEntry', 'app/Facade'],
function(React, identity,
         NewFuelPurchaseEntry, Facade) {
    class Root extends React.Component {
        constructor() {
            super();
            this.state = { user: null };
        }
        render() {
            if (!this.state.user)
                return <div><Facade /></div>;
            return <NewFuelPurchaseEntry />;
        }
        componentDidMount() {
            identity.subscribe({
                localIdentity: user => {
                    this.setState({ user: user });
                }
            }, this);
        }
        componentWillUnmount() {
            identity.unsubscribe(this);
        }
    }
    return Root;
});
