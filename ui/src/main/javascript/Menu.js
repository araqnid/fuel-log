import React from "react";
import routing from "./routing";
import BUS from "./message-bus";

export default class Menu extends React.Component {
    constructor(props) {
        super(props);
        this.state = { display: null };
    }
    render() {
        return <div>the menu</div>
    }
    componentDidMount() {
        BUS.subscribe("Navigate.ToEmpty", data => {
            this.setState({ display: null });
        }, this);

        BUS.subscribe("Router.HashChange", hash => {
            let routeString = hash;
            if (routeString === null) routeString = '';
            if (routeString.substring(0, 1) === '#') routeString = routeString.substring(1);
            routing(routeString);
        }, this);
    }
    componentWillUnmount() {
        BUS.unsubscribe(this);
    }
}
