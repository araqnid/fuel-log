import BaseStore from "./BaseStore";
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

const log = logFactory("RealmIdentityStore");

export class RealmIdentityStore extends StoreBase {
    constructor() {
        super();
        this._realmProviders = {GOOGLE: new GoogleIdentityProvider(), FACEBOOK: new FacebookIdentityProvider()};
        this._localUserIdentity = new Datum(this, 'localUserIdentity');
        this._googleAvailable = new Datum(this, 'googleAvailable');
        this._facebookAvailable = new Datum(this, 'facebookAvailable');
    }

    get localUserIdentity() {
        return this._localUserIdentity.value();
    }

    get googleAvailable() {
        return this._googleAvailable.value();
    }

    get facebookAvailable() {
        return this._facebookAvailable.value();
    }

    begin() {
        this._realmProviders.GOOGLE.subscribe(this, 'available', v => this._googleAvailable.value = v);
        this._realmProviders.FACEBOOK.subscribe(this, 'available', v => this._facebookAvailable.value = v);
        Object.values(this._realmProviders).forEach(p => p.begin());
        this.callAjax({
            url: '/_api/user/identity',
            method: 'GET',
        }).then(data => {
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
                            this.callAjax({
                                url: '/_api/user/identity',
                                method: 'DELETE'
                            });
                            return null;
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
            .then(() => {
                return this.callAjax({
                    url: '/_api/user/identity',
                    method: 'DELETE'
                });
            })
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

export default class IdentityStore extends BaseStore {
    constructor() {
        super("Identity");
        this._underlying = new RealmIdentityStore();
    }

    start() {
        this._underlying.subscribe(this, "localUserIdentity", v => {
            if (v) {
                this._storeLocalIdentity(v);
            }
            else {
                this._clearLocalIdentity();
            }
        });
        this._underlying.subscribe(this, "googleAvailable", v => {
            this.bus.dispatch("googleAvailable", v);
        });
        this._underlying.subscribe(this, "facebookAvailable", v => {
            this.bus.dispatch("facebookAvailable", v);
        });
        this.bus.dispatch("googleAvailable", false);
        this.bus.dispatch("facebookAvailable", false);
        this._underlying.begin();
    }
    beginGoogleSignIn() {
        this._underlying.beginGoogleSignIn();
    }
    beginFacebookSignIn() {
        this._underlying.beginFacebookSignIn();
    }
    beginSignOut() {
        this._underlying.signOut();
    }
    _storeLocalIdentity(data) {
        const localIdentity = { userId: data.user_id, name: data.name, picture: data.picture, realm: data.realm };
        this.bus.dispatch('localIdentity', localIdentity);
    }
    _clearLocalIdentity() {
        this.bus.dispatch('localIdentity', null);
    }
}
