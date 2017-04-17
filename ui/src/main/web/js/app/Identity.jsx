import React from "react";
import {identity} from "app/stores";

const identity_beginSignOut = identity.beginSignOut.bind(identity);
const identity_beginGoogleSignIn = identity.beginGoogleSignIn.bind(identity);
const identity_beginFacebookSignIn = identity.beginFacebookSignIn.bind(identity);
const suppress = (e) => {
    e.preventDefault();
    return false;
};

export const SignedIn = props => {
    const prefix = "Signed in";
    const withRealm = props.realm === "GOOGLE" ? " with Google" : props.realm === "FACEBOOK" ? " with Facebook" : "";
    const suffix = " as " + props.name;
    const text = prefix + withRealm + suffix;
    return <div>
        <form className="navbar-form navbar-right" onSubmit={ suppress }>
            <button type="submit" className="btn btn-success navbar-right" onClick={ identity_beginSignOut }>Sign out</button>
        </form>
        <img src={props.picture} width="36" height="36" className="navbar-right" style={{marginTop: "8px", marginLeft: "8px"}} />
        <p className="navbar-text navbar-right">{text}</p>
    </div>;
};

export const SignedOut = props => {
    const signInButtons = [];
    if (props.offerGoogle) signInButtons.push(<button key="google" onClick={ identity_beginGoogleSignIn } type="submit" style={{marginLeft: "8px"}} className="btn btn-success">Sign in with Google</button>);
    if (props.offerFacebook) signInButtons.push(<button key="facebook" onClick={ identity_beginFacebookSignIn } type="submit" style={{marginLeft: "8px"}} className="btn btn-success">Sign in with Facebook</button>);
    return <div><form className="navbar-form navbar-right" onSubmit={suppress}>{signInButtons}</form></div>;
};

export default class Identity extends React.Component {
    constructor() {
        super();
        this.state = { signedInState: 'pending', user: null, offerGoogle: false, offerFacebook: false };
    }
    render() {
        const state = this.state;
        if (state.signedInState === 'signed-in') {
            return <SignedIn {...state.user}/>;
        }
        else if (state.signedInState === 'signed-out') {
            return <SignedOut {...state} />
        }
        else {
            return <div></div>;
        }
    }
    componentDidMount() {
        identity.subscribe({
            googleUser: googleUser => {
                this.setState({ offerGoogle: true });
            },
            facebookLoginStatus: facebookLoginStatus => {
                this.setState({ offerFacebook: true });
            },
            localIdentity: user => {
                if (user) {
                    this.setState({ signedInState: 'signed-in', user: user })
                }
                else {
                    this.setState({ signedInState: 'signed-out', user: null });
                }
            }
        }, this);
    }
    componentWillUnmount() {
        identity.unsubscribe(this);
    }
}
