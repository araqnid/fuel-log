import axios from "axios";
import {combineReducers} from "redux";
import {bindActionPayload} from "../util/Stores";
import {logFactory} from "../util/ConsoleLog";
import GoogleIdentityProvider from "./GoogleIdentityProvider";
import FacebookIdentityProvider from "./FacebookIdentityProvider";

// IdentityProvider interface:
//  start() // void
//  stop() // void
//  probe() // Promise<Boolean>
//  autoLogin() // Promise<LocalIdentity>
//  confirmUser(userInfo) // Promise<LocalIdentity>
//  signOut() // Promise<?>
//  signIn() // Promise<LocalIdentity>

const log = logFactory("IdentityStore");

export const reducer = combineReducers({
    googleAvailable: bindActionPayload("IdentityStore/googleAvailable", false),
    facebookAvailable: bindActionPayload("IdentityStore/facebookAvailable", false),
    localUserIdentity: bindActionPayload("IdentityStore/localUserIdentity", null)
});

export const actions = dispatch => ({
    beginGoogleSignIn() {
        dispatch(({stores}) => {
            stores.identity.signInWithGoogle();
        })
    },

    beginFacebookSignIn() {
        dispatch(({stores}) => {
            stores.identity.signInWithFacebook();
        })
    },

    beginSignOut() {
        dispatch(({stores}) => {
            stores.identity.signOut();
        })
    }
});

export default class IdentityStore {
    constructor(redux) {
        this._redux = redux;
        this._realmProviders = {
            GOOGLE: new GoogleIdentityProvider(),
            FACEBOOK: new FacebookIdentityProvider()
        };
    }

    start() {
        this._realmProviders.GOOGLE.onAvailable(() => this.dispatch({ type: "IdentityStore/googleAvailable", payload: true }));
        this._realmProviders.FACEBOOK.onAvailable(() => this.dispatch({ type: "IdentityStore/facebookAvailable", payload: true }));
        this._realmProviders.GOOGLE.onUserUpdate(userInfo => this.dispatch({ type: "IdentityStore/localUserIdentity", payload: userInfo }));
        Object.values(this._realmProviders).forEach(p => p.start());
        this._launch();
    }

    stop() {
        Object.values(this._realmProviders).forEach(p => p.stop());
    }

    async _launch() {
        let {data: {user_info: userInfo}} = await axios.get("_api/user/identity");
        if (userInfo) {
            const realm = this._realmProviders[userInfo.realm];
            if (realm) {
                log.info("last known user identity is in known realm, confirm it", userInfo);
                const confirmed = await realm.confirmUser(userInfo);
                if (confirmed) {
                    log.info("User details confirmed");
                    userInfo = confirmed;
                }
                else {
                    log.info("User details not confirmed");
                    await axios.delete("_api/user/identity");
                    userInfo = null;
                }
            }
            else {
                log.warn("last known user identity is in unknown realm");
                await axios.delete("_api/user/identity");
                userInfo = null;
            }
        }
        else {
            log.info("Asking realm providers to probe for identity");
            const names = Object.keys(this._realmProviders);
            const values = await Promise.all(names.map(name => this._realmProviders[name].probe()));

            log.info("Collected probe results", values);
            const result = {};
            names.forEach((name, index) => {
                result[name] = values[index];
            });

            log.info("Assembled probe results", result);
            if (result.GOOGLE) {
                userInfo = await this._realmProviders.GOOGLE.autoLogin();
            }
            else if (result.FACEBOOK) {
                userInfo = await this._realmProviders.FACEBOOK.autoLogin();
            }
            else {
                userInfo = null;
            }
        }

        log.info("final user: ", userInfo);
        this.dispatch({ type: "IdentityStore/localUserIdentity", payload: userInfo });
    }

    get _reduxUserInfo() {
        const userIdentity = this._redux.getState().identity;
        return userIdentity ? userIdentity.localUserIdentity : null;
    }

    signInWithGoogle() {
        return this._signIn(this._realmProviders.GOOGLE);
    }

    signInWithFacebook() {
        return this._signIn(this._realmProviders.FACEBOOK);
    }

    async signOut() {
        const userInfo = this._reduxUserInfo;
        if (!userInfo) return;
        log.info("Sign out", userInfo);
        await this._realmProviders[userInfo.realm].signOut();
        await axios.delete("/_api/user/identity");
        this.dispatch({ type: "IdentityStore/localUserIdentity", payload: null });
    }

    async _signIn(realm) {
        const userInfo = await realm.signIn();
        log.info("completed realm sign in", userInfo);
        this.dispatch({ type: "IdentityStore/localUserIdentity", payload: userInfo });
    }

    dispatch(action) {
        this._redux.dispatch(action);
    }
}
