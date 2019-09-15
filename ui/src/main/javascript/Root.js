import React, {useCallback, useContext, useEffect, useState} from "react";
import Observable from "zen-observable";
import Identity from "./Identity";
import NewFuelPurchaseEntry from "./NewFuelPurchaseEntry";
import FuelPurchaseList from "./FuelPurchaseList";
import * as ajax from "./util/Ajax";
import autoRefresh from "./util/autoRefresh";
import Throbber from "./Throbber";

export const IdentityStoreContext = React.createContext(null /* new IdentityStore() */);

const Root = ({}) => {
    const identityStore = useContext(IdentityStoreContext);
    const [userIdentity, setUserIdentity] = useState(identityStore.localUserIdentity);
    const [googleAvailable, setGoogleAvailable] = useState(false);
    const [facebookAvailable, setFacebookAvailable] = useState(false);
    useEffect(() => {
        const subscription = Observable.from(identityStore).subscribe(
            ({type, payload, error}) => {
                switch (type) {
                    case "localUserIdentity":
                        setUserIdentity(payload);
                        break;
                    case "googleAvailable":
                        setGoogleAvailable(payload);
                        break;
                    case "facebookAvailable":
                        setFacebookAvailable(payload);
                        break;
                    default:
                        if (error) {
                            console.warn("ignoring IdentityStore error", type, payload);
                        }
                        else {
                            console.log("ignoring IdentityStore message", type, payload);
                        }
                        break;
                }
            }
        );
        return () => {
            subscription.unsubscribe();
        }
    }, []);
    const signInWithGoogle = useCallback(() => {
        identityStore.signInWithGoogle();
    }, []);
    const signInWithFacebook = useCallback(() => {
        identityStore.signInWithFacebook();
    }, []);
    const signOut = useCallback(() => {
        console.log("sign out");
        identityStore.signOut().then(() => {
            console.log("sign out complete");
        });
    }, []);
    return (
        <div>
            <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
                <a className="navbar-brand" href="/#">Fuel Log</a>
                <button className="navbar-toggler" type="button" data-toggle="collapse"
                        data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent"
                        aria-expanded="false"
                        aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"/>
                </button>

                <div className="collapse navbar-collapse" id="navbarSupportedContent">
                    <ul className="navbar-nav mr-auto">
                    </ul>

                    <Identity userIdentity={userIdentity}
                              beginGoogleSignIn={googleAvailable ? signInWithGoogle : null}
                              beginFacebookSignIn={facebookAvailable ? signInWithFacebook : null}
                              beginSignOut={signOut}/>
                </div>
            </nav>
            <Content userIdentity={userIdentity}/>
        </div>
    );
};

const Content = ({userIdentity}) => {
    if (userIdentity) {
        const { user_id: userId } = userIdentity;
        return <UserContent key={userId} userId={userId}/>;
    } else {
        return (
            <div>
                <div className="jumbotron">
                    <div className="container"><h1>Fuel Log</h1></div>
                </div>
            </div>
        );
    }
};

const UserContent = ({userId}) => {
    const [preferences, setPreferences] = useState(null);
    useEffect(() => {
        const subscription = autoRefresh(60000)(ajax.get("user/preferences")).subscribe(
            ({currency, distance_unit: distanceUnit, fuel_volume_unit: fuelVolumeUnit}) => {
                setPreferences({currency, distanceUnit, fuelVolumeUnit});
            }
        );
        return () => {
            subscription.unsubscribe();
            setPreferences(null);
        }
    }, [userId]);
    const [saveCounter, setSaveCounter] = useState(0);
    const onPurchaseSaved = useCallback(() => {
        setSaveCounter(c => c + 1);
    }, []);

    if (!preferences) {
        return <Throbber />;
    }

    return (
        <div className="container">
            <div className="row">
                <NewFuelPurchaseEntry preferences={preferences} onPurchaseSaved={onPurchaseSaved}/>
                <FuelPurchaseList preferences={preferences} saveCounter={saveCounter}/>
            </div>
        </div>
    );
};

export default Root;
