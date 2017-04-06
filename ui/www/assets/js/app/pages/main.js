define(['jquery', 'react', 'react-dom',
        'app/Root', 'app/Identity', 'app/Bus', 'app/stores/identity',
        'bootstrap'],
function main$$init($, React, ReactDOM,
                    Root, Identity, Bus, identity) {
    window.BUS = new Bus();
    ReactDOM.render(<Root/>, document.getElementById("component.Root"));
    ReactDOM.render(<Identity/>, document.getElementById("component.Identity"));
    identity.start();
    return "main";
});
