define(['jquery', 'react', 'react-dom',
        'app/Status', 'app/Bus',
        'bootstrap'],
    function status$$init($, React, ReactDOM,
                        Status, Bus, identity) {
        window.BUS = new Bus();
        ReactDOM.render(<Status/>, document.getElementById("component.Status"));
        return "status";
    });
