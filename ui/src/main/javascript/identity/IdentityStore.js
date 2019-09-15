import Observable from "zen-observable";
import {logFactory} from "../util/ConsoleLog";
import GoogleIdentityProvider from "./GoogleIdentityProvider";
import FacebookIdentityProvider from "./FacebookIdentityProvider";
import {localAxios} from "../util/Ajax";

// IdentityProvider interface:
//  start() // void
//  stop() // void
//  probe() // Promise<Boolean>
//  autoLogin() // Promise<LocalIdentity>
//  confirmUser(userInfo) // Promise<LocalIdentity>
//  signOut() // Promise<?>
//  signIn() // Promise<LocalIdentity>

const log = logFactory("IdentityStore");

export default class IdentityStore {
    constructor(googleEnabled = true, facebookEnabled = true) {
        this._realmProviders = {};
        if (googleEnabled)
            this._realmProviders.GOOGLE = new GoogleIdentityProvider();
        if (facebookEnabled)
            this._realmProviders.FACEBOOK = new FacebookIdentityProvider();
        this._subscribers = new Set();
        this.localUserIdentity = null;
    }

    start() {
        if (this._realmProviders.GOOGLE) {
            this._realmProviders.GOOGLE.onAvailable(() => this._emit("googleAvailable", true));
            this._realmProviders.GOOGLE.onUserUpdate(userInfo => this._emit("localUserIdentity", userInfo));
        }
        else {
            this._emit("googleAvailable", false);
        }
        if (this._realmProviders.FACEBOOK) {
            this._realmProviders.FACEBOOK.onAvailable(() => this._emit("facebookAvailable", true));
        }
        else {
            this._emit("facebookAvailable", false);
        }
        Object.values(this._realmProviders).forEach(p => p.start());
        this._launch();
    }

    stop() {
        Object.values(this._realmProviders).forEach(p => p.stop());
    }

    async _launch() {
        const {data: {user_info: provisionalUserInfo}} = await localAxios.get("_api/user/identity");

        const userInfo = await (provisionalUserInfo ? this._confirm(provisionalUserInfo) : this._probe());

        log.info("final user: ", userInfo);
        this._emit("localUserIdentity", userInfo);
        this.localUserIdentity = userInfo;
    }

    async _confirm(provisionalUserInfo) {
        const realm = this._realmProviders[provisionalUserInfo.realm];
        if (realm) {
            log.info("last known user identity is in known realm, confirm it", provisionalUserInfo);
            const confirmed = await realm.confirmUser(provisionalUserInfo);
            if (confirmed) {
                log.info("User details confirmed");
                return confirmed;
            }
            else {
                log.info("User details not confirmed");
                await localAxios.delete("_api/user/identity");
                return null;
            }
        }
        else {
            log.warn("last known user identity is in unknown realm");
            await localAxios.delete("_api/user/identity");
            return null;
        }
    }

    async _probe() {
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
            return this._realmProviders.GOOGLE.autoLogin();
        }
        else if (result.FACEBOOK) {
            return this._realmProviders.FACEBOOK.autoLogin();
        }
        else {
            return null;
        }
    }

    signInWithGoogle() {
        return this._signIn(this._realmProviders.GOOGLE);
    }

    signInWithFacebook() {
        return this._signIn(this._realmProviders.FACEBOOK);
    }

    async signOut() {
        if (!this.localUserIdentity) return;
        log.info("Sign out", this.localUserIdentity);
        await this._realmProviders[this.localUserIdentity.realm].signOut();
        await localAxios.delete("/_api/user/identity");
        this._emit("localUserIdentity", null);
        this.localUserIdentity = null;
    }

    async _signIn(realm) {
        const userInfo = await realm.signIn();
        log.info("completed realm sign in", userInfo);
        this._emit("localUserIdentity", userInfo);
        this.localUserIdentity = userInfo;
    }

    _emit(type, payload, error = false) {
        for (const observer of this._subscribers) {
            observer.next({ type, payload, error });
        }
    }

    [Symbol.observable]() {
        return new Observable(observer => {
            this._subscribers.add(observer);
            return () => {
                this._subscribers.delete(observer);
            };
        });
    }
}
