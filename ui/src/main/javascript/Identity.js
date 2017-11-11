import React from "react";
import {connect} from "react-redux";
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

const Identity = ({ signedInState, user, offerGoogle, offerFacebook }) => {
    switch (signedInState) {
        case "signed-in":
            return <SignedIn {...user} />;
        case "signed-out":
            return <SignedOut offerGoogle={offerGoogle} offerFacebook={offerFacebook}/>;
        default:
            return null;
    }
};

export default connect(
    ({ identity: { googleAvailable, facebookAvailable, localUserIdentity } }) => ({
        offerGoogle: googleAvailable,
        offerFacebook: facebookAvailable,
        user: localUserIdentity,
        signedInState: localUserIdentity ? 'signed-in' : 'signed-out'
    }),
    (dispatch) => ({})
)(Identity);
