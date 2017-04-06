define(['react', 'lodash', 'app/stores/status'],
function(React, _, store) {
    const store_kick = store.kick.bind(store);
    const store_pause = store.pause.bind(store);
    const store_unpause = store.unpause.bind(store);
    const RefreshState = props => {
        if (props.paused) {
            return <div id="refresh-state" className="paused"><button onClick={store_kick}>Refresh</button><button onClick={store_unpause}>Start auto-refresh</button></div>;
        }
        else {
            return <div id="refresh-state" className="unpaused">Auto-refresh interval: {props.interval} <button onClick={store_pause}>Stop auto-refresh</button></div>;
        }
    };
    const StatusComponent = props => {
        return <li className={ "status-component priority-" + props.priority.toLowerCase() }>
            <span className="label">{props.priority === "INFO" ? props.label : props.label + " - " + props.priority}</span>
            <span className="value">{props.text}</span>
        </li>;
    };
    const StatusDetails = props => {
        const headlineStatus = props.version && props.version.title ? props.version.title + " " + props.version.version + " - " + props.statusPage.status : props.statusPage.status;
        const componentItems = [];
        _.forOwn(props.statusPage.components, (comp, id) => {
            componentItems.push(<StatusComponent key={id} id={id} label={comp.label} priority={comp.priority} text={comp.text} />);
        });
        return <div className={ "status-page priority-" + props.statusPage.status.toLowerCase() }>
            <h1>{headlineStatus}</h1>
            <ul>{componentItems}</ul>
        </div>;
    };
    const LoadingStatusDetails = props => {
        if (!props.statusPage || !props.version) {
            return <div>Loading...</div>;
        }
        return <StatusDetails {...props} />;
    };
    class Status extends React.Component {
        constructor(props) {
            super(props);
            this.state = { statusPage: null, refreshState: null, readiness: null, version: null, loadingError: null };
        }
        render() {
            if (!this.state.refreshState) {
                return <div key="absent"></div>;
            }
            if (this.state.loadingError !== null) {
                return <div key="error">Failed to load status: { this.state.loadingError }</div>;
            }
            return <div key="present">
                <LoadingStatusDetails version={this.state.version} statusPage={this.state.statusPage} />
                { this.state.readiness !== null && this.state.readiness.toLowerCase() === "ready"
                    ? <div className="readiness readiness-ready">Application ready</div>
                    : <div className="readiness readiness-other">Application NOT ready</div> }
                <RefreshState {...this.state.refreshState} />
            </div>;
        }
        componentDidMount() {
            store.subscribe({
                status: d => {
                    this.setState({ statusPage: d, loadingError: null })
                },
                readiness: d => {
                    this.setState({ readiness: d, loadingError: null })
                },
                version: d => {
                    this.setState({ version: d, loadingError: null })
                },
                "status.error": ex => {
                    this.setState({ status: null, version: null, readiness: null, loadingError: ex })
                },
                "readiness.error": ex => {
                    this.setState({ status: null, version: null, readiness: null, loadingError: ex })
                },
                "version.error": ex => {
                    this.setState({ status: null, version: null, readiness: null, loadingError: ex })
                },
                refreshState: d => {
                    this.setState({ refreshState: d })
                }
            }, this)
        }
        componentWillUnmount() {
            store.unsubscribe(this)
        }
    }
    return Status;
});
