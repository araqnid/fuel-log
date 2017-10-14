import React from "react";

export function suppressUpdates(Component) {
    const wrapped = class extends React.Component {
        render() {
            return <Component {...this.props} />;
        }

        shouldComponentUpdate(nextProps) {
            return false;
        }
    };

    wrapped.displayName = "suppressUpdates(" + (Component.displayName || Component.name || "Component") + ")";

    return wrapped;
}
