import axios from "axios";
import {combineReducers} from "redux";
import {bindActionPayload} from "../util/Stores";
import {logFactory} from "../util/ConsoleLog";
import GoogleIdentityProvider from "./GoogleIdentityProvider";
import FacebookIdentityProvider from "./FacebookIdentityProvider";

// IdentityProvider interface:
//  probe() // Promise<Boolean>
//  autoLogin() // Promise<LocalIdentity>
//  confirmUser(userInfo) // Promise<LocalIdentity>
//  signOut() // Promise<?>
//  signIn() // Promise<LocalIdentity>

const log = logFactory("IdentityStore");

export const reducer = combineReducers({
    googleAvailable: bindActionPayload("IdentityStore/googleAvailable", false),
    facebookAvailable: bindActionPayload("IdentityStore/facebookAvailable", false),
    localUserIdentity: bindActionPayload("IdentityStore/localUserIdentity", null),
    realmChange: (state = null, action) => {
        switch (action.type) {
            case "IdentityStore/realmChange":
                return action.payload;
            case "IdentityStore/localUserIdentity":
                const userInfo = action.error ? null : action.payload;
                if (userInfo) {
                    return userInfo.realm;
                }
                else {
                    return null;
                }
            default:
                return state;
        }
    }
});

export const actions = dispatch => ({
    beginGoogleSignIn() {
        dispatch({ type: "IdentityStore/realmChange", payload: "GOOGLE" });
    },

    beginFacebookSignIn() {
        dispatch({ type: "IdentityStore/realmChange", payload: "FACEBOOK" });
    },

    beginSignOut() {
        dispatch({ type: "IdentityStore/realmChange", payload: null });
    }
});

export default class IdentityStore {
    constructor(redux) {
        this._redux = redux;
        this._reduxUnsubscribe = null;
        this._realmProviders = {
            GOOGLE: new GoogleIdentityProvider(),
            FACEBOOK: new FacebookIdentityProvider()
        };
        this._inProgress = false;
    }

    start() {
        this._reduxUnsubscribe = this._redux.subscribe(this.onReduxAction.bind(this));
        this._realmProviders.GOOGLE.subscribe(this, 'available', v => this.dispatch({ type: "IdentityStore/googleAvailable", payload: v }));
        this._realmProviders.FACEBOOK.subscribe(this, 'available', v => this.dispatch({ type: "IdentityStore/facebookAvailable", payload: v }));
        Object.values(this._realmProviders).forEach(p => p.begin());
        axios.get("_api/user/identity")
            .then(({data: {user_info: userInfo}}) => {
                if (userInfo) {
                    const realm = this._realmProviders[userInfo.realm];
                    if (realm) {
                        log.info("last known user identity is in known realm, confirm it", userInfo);
                        return realm.confirmUser(userInfo).then(confirmed => {
                            if (confirmed) {
                                log.info("User details confirmed");
                                return confirmed;
                            }
                            else {
                                log.info("User details not confirmed");
                                return axios.delete("_api/user/identity").then(() => null);
                            }
                        });
                    }
                    else {
                        log.warn("last known user identity is in unknown realm");
                        return axios.delete("_api/user/identity").then(() => null);
                    }
                }
                else {
                    log.info("Asking realm providers to probe for identity");
                    const names = Object.keys(this._realmProviders);
                    return Promise.all(names.map(name => this._realmProviders[name].probe()))
                        .then(values => {
                            log.info("Collected probe results", values);
                            const result = {};
                            names.forEach((name, index) => {
                                result[name] = values[index];
                            });
                            return result;
                        })
                        .then(result => {
                            log.info("Assembled probe results", result);
                            if (result.GOOGLE) {
                                return this._realmProviders.GOOGLE.autoLogin();
                            }
                            else if (result.FACEBOOK) {
                                return this._realmProviders.FACEBOOK.autoLogin();
                            }
                            else {
                                return null;
                            }
                        });
                }
            })
            .then(userInfo => {
                log.info("final user: ", userInfo);
                this.dispatch({ type: "IdentityStore/localUserIdentity", payload: userInfo });
            })
    }

    stop() {
        Object.values(this._realmProviders).forEach(p => p.abort());
        if (this._reduxUnsubscribe) {
            this._reduxUnsubscribe();
            this._reduxUnsubscribe = null;
        }
    }

    get _reduxUserInfo() {
        const userIdentity = this._redux.getState().identity;
        return userIdentity ? userIdentity.localUserIdentity : null;
    }

    get _reduxRealmChange() {
        const userIdentity = this._redux.getState().identity;
        return userIdentity ? userIdentity.realmChange : null;
    }

    onReduxAction() {
        const userInfo = this._reduxUserInfo;
        const currentRealm = userInfo ? userInfo.realm : null;

        if (this._reduxRealmChange !== currentRealm && !this._inProgress) {
            log.info("Change realm", this._reduxRealmChange);
            this._inProgress = true;
            const realmChange = this._reduxRealmChange ? this._beginSignIn(this._realmProviders[this._reduxRealmChange]) : this._beginSignOut();
            realmChange.then(() => {
                log.info("Finished realm change");
                this._inProgress = false;
            })
        }
    }

    _beginSignOut() {
        const userInfo = this._reduxUserInfo;
        if (!userInfo) return;
        log.info("Sign out", userInfo);
        return this._realmProviders[userInfo.realm].signOut()
            .then(() => axios.delete("/_api/user/identity"))
            .then(() => {
                this.dispatch({ type: "IdentityStore/localUserIdentity", payload: null });
            });
    }

    _beginSignIn(realm) {
        return realm.signIn().then(userInfo => {
            log.info("completed realm sign in", userInfo);
            this.dispatch({ type: "IdentityStore/localUserIdentity", payload: userInfo });
        });
    }

    dispatch(action) {
        this._redux.dispatch(action);
    }
}
