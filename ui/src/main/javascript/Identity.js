import React from "react";

const suppress = (e) => {
    e.preventDefault();
    return false;
};

const SignedIn = ({userIdentity: {realm, name, picture}, beginSignOut}) => {
    const prefix = "Signed in";
    const withRealm = realm === "GOOGLE" ? " with Google" : realm === "FACEBOOK" ? " with Facebook" : "";
    const suffix = " as " + name;
    const text = prefix + withRealm + suffix;
    return (
        <ul className="navbar-nav">
            <li className="nav-item">
                <a className="nav-link" href="#">{text}
                </a>
            </li>
            <li className="nav-item">
                <img src={picture} width="36" height="36" className="navbar-right"
                     style={{display: "inline-block", marginRight: "8px"}}/>
            </li>
            <li className="nav-item">
                <form className="form-inline my-2 my-lg-0" onSubmit={suppress}>
                    <button type="submit" className="btn btn-light my-2 my-sm-0" onClick={beginSignOut}>Sign out
                    </button>
                </form>
            </li>
        </ul>
    );
};

const SignedOut = ({beginGoogleSignIn, beginFacebookSignIn}) => {
    const signInButtons = [
        {key: "google", begin: beginGoogleSignIn, name: "Google"},
        {key: "facebook", begin: beginFacebookSignIn, name: "Facebook"},
    ];
    return (
        <ul className="navbar-nav">
            <li className="nav-item">
                <form className="form-inline my-2 my-lg-0" onSubmit={suppress}>
                    {signInButtons.filter(it => !!it.begin).map(it => (
                        <button key={it.key} onClick={it.begin} type="submit" style={{marginLeft: "8px"}}
                                className="btn btn-light">Sign in with {it.name}</button>
                    ))}
                </form>
            </li>
        </ul>
    );
};

const Identity = ({userIdentity, beginGoogleSignIn, beginFacebookSignIn, beginSignOut}) => {
    if (userIdentity) {
        return <SignedIn userIdentity={userIdentity} beginSignOut={beginSignOut}/>;
    } else {
        return <SignedOut beginFacebookSignIn={beginFacebookSignIn} beginGoogleSignIn={beginGoogleSignIn}/>;
    }
};

export default Identity;
