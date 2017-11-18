import React from "react";
import {connect} from "react-redux";
import {actions as identityActions} from "./stores/IdentityStore";

const suppress = (e) => {
    e.preventDefault();
    return false;
};

export const SignedIn = ({realm, name, picture, beginSignOut}) => {
    const prefix = "Signed in";
    const withRealm = realm === "GOOGLE" ? " with Google" : realm === "FACEBOOK" ? " with Facebook" : "";
    const suffix = " as " + name;
    const text = prefix + withRealm + suffix;
    return <div>
        <form className="navbar-form navbar-right" onSubmit={ suppress }>
            <button type="submit" className="btn btn-success navbar-right" onClick={ beginSignOut }>Sign out</button>
        </form>
        <img src={picture} width="36" height="36" className="navbar-right" style={{marginTop: "8px", marginLeft: "8px"}} />
        <p className="navbar-text navbar-right">{text}</p>
    </div>;
};

export const SignedOut = ({beginGoogleSignIn, beginFacebookSignIn}) => {
    const signInButtons = [];
    if (beginGoogleSignIn) signInButtons.push(<button key="google" onClick={ beginGoogleSignIn } type="submit" style={{marginLeft: "8px"}} className="btn btn-success">Sign in with Google</button>);
    if (beginFacebookSignIn) signInButtons.push(<button key="facebook" onClick={ beginFacebookSignIn } type="submit" style={{marginLeft: "8px"}} className="btn btn-success">Sign in with Facebook</button>);
    return <div><form className="navbar-form navbar-right" onSubmit={suppress}>{signInButtons}</form></div>;
};

const Identity = ({ signedInState, user, offerGoogle, offerFacebook, identityActions }) => {
    switch (signedInState) {
        case "signed-in":
            return <SignedIn {...user} beginSignOut={identityActions.beginSignOut} />;
        case "signed-out":
            return <SignedOut beginGoogleSignIn={offerGoogle ? identityActions.beginGoogleSignIn : null}
                              beginFacebookSignIn={offerFacebook ? identityActions.beginFacebookSignIn : null }/>;
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
    (dispatch) => ({ identityActions: identityActions(dispatch) })
)(Identity);
