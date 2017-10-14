import React from "react";
import {identity} from "./stores";

const identity_beginSignOut = identity.signOut.bind(identity);
const identity_beginGoogleSignIn = identity.beginGoogleSignIn.bind(identity);
const identity_beginFacebookSignIn = identity.beginFacebookSignIn.bind(identity);
const suppress = (e) => {
    e.preventDefault();
    return false;
};

export const SignedIn = ({realm, name, picture}) => {
    const prefix = "Signed in";
    const withRealm = realm === "GOOGLE" ? " with Google" : realm === "FACEBOOK" ? " with Facebook" : "";
    const suffix = " as " + name;
    const text = prefix + withRealm + suffix;
    return <div>
        <form className="navbar-form navbar-right" onSubmit={ suppress }>
            <button type="submit" className="btn btn-success navbar-right" onClick={ identity_beginSignOut }>Sign out</button>
        </form>
        <img src={picture} width="36" height="36" className="navbar-right" style={{marginTop: "8px", marginLeft: "8px"}} />
        <p className="navbar-text navbar-right">{text}</p>
    </div>;
};

export const SignedOut = ({offerGoogle, offerFacebook}) => {
    const signInButtons = [];
    if (offerGoogle) signInButtons.push(<button key="google" onClick={ identity_beginGoogleSignIn } type="submit" style={{marginLeft: "8px"}} className="btn btn-success">Sign in with Google</button>);
    if (offerFacebook) signInButtons.push(<button key="facebook" onClick={ identity_beginFacebookSignIn } type="submit" style={{marginLeft: "8px"}} className="btn btn-success">Sign in with Facebook</button>);
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
            return <div/>;
        }
    }
    componentDidMount() {
        identity.googleAvailable.subscribe(this, v => {
            this.setState({ offerGoogle: v });
        });
        identity.facebookAvailable.subscribe(this, v => {
            this.setState({ offerFacebook: v });
        });
        identity.localUserIdentity.subscribe(this, user => {
            if (user) {
                this.setState({ signedInState: 'signed-in', user: user })
            }
            else {
                this.setState({ signedInState: 'signed-out', user: null });
            }
        });
    }
    componentWillUnmount() {
        identity.unsubscribeAll(this);
    }
}
