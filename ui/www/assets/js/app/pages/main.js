define(['jquery', 'react', 'react-dom',
        'app/Root', 'app/Identity', 'app/Bus', 'app/stores/identity', 'app/stores/purchases',
        'bootstrap', '../../../css/styles.css'],
function main$$init($, React, ReactDOM,
                    Root, Identity, Bus, identity, purchases) {
    window.BUS = new Bus();
    ReactDOM.render(<Root/>, document.getElementById("component.Root"));
    ReactDOM.render(<Identity/>, document.getElementById("component.Identity"));
    identity.start();
    purchases.start();
    return "main";
});
