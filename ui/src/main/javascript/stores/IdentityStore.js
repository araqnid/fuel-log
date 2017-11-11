import {Datum, StoreBase} from "../util/Stores";
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

export default class IdentityStore extends StoreBase {
    constructor() {
        super();
        this._realmProviders = {GOOGLE: new GoogleIdentityProvider(), FACEBOOK: new FacebookIdentityProvider()};
        this._localUserIdentity = new Datum(this, 'localUserIdentity');
        this._googleAvailable = new Datum(this, 'googleAvailable');
        this._facebookAvailable = new Datum(this, 'facebookAvailable');
    }

    get localUserIdentity() {
        return this._localUserIdentity.facade();
    }

    get googleAvailable() {
        return this._googleAvailable.facade();
    }

    get facebookAvailable() {
        return this._facebookAvailable.facade();
    }

    begin() {
        this._realmProviders.GOOGLE.subscribe(this, 'available', v => this._googleAvailable.value = v);
        this._realmProviders.FACEBOOK.subscribe(this, 'available', v => this._facebookAvailable.value = v);
        Object.values(this._realmProviders).forEach(p => p.begin());
        this.get("_api/user/identity").then(({data}) => {
            const userInfo = data.user_info;
            if (userInfo) {
                const realm = this._realmProviders[userInfo.realm];
                if (realm) {
                    return realm.confirmUser(userInfo).then(confirmed => {
                        if (confirmed) {
                            log.info("User details confirmed", confirmed);
                            this._localUserIdentity.value = confirmed;
                            return confirmed;
                        }
                        else {
                            log.info("User details not confirmed");
                            this._localUserIdentity.value = null;
                            return this.callDelete("_api/user/identity").then(() => null);
                        }
                    });
                }
                else {
                    log.warn("user in unknown realm", userInfo);
                    return Promise.resolve(null);
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
                    })
                    .then(userInfo => {
                        log.info("User after auto-login", userInfo);
                        this._localUserIdentity.value = userInfo;
                    });
            }
        });
    }

    signOut() {
        const userInfo = this._localUserIdentity.value;
        if (!userInfo) return;
        log.info("Sign out", userInfo);
        this._realmProviders[userInfo.realm].signOut()
            .then(() => this.callDelete("/_api/user/identity"))
            .then(() => {
                this._localUserIdentity.value = null;
            });
    }

    beginGoogleSignIn() {
        this._beginSignIn(this._realmProviders.GOOGLE);
    }

    beginFacebookSignIn() {
        this._beginSignIn(this._realmProviders.FACEBOOK);
    }

    _beginSignIn(realm) {
        realm.signIn().then(userInfo => {
            log.info("completed realm sign in", userInfo);
            this._localUserIdentity.value = userInfo;
        });
    }

    abort() {
        super.abort();
        Object.values(this._realmProviders).forEach(p => p.abort());
    }
}
