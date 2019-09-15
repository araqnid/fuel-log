import Observable from "zen-observable";
import {logFactory} from "../util/ConsoleLog";
import GoogleIdentityProvider from "./GoogleIdentityProvider";
import FacebookIdentityProvider from "./FacebookIdentityProvider";
import {localAxios} from "../util/Ajax";

// IdentityProvider interface:
//  onAvailable(listener) // listener called when realm is able to accept calls
//  onUserUpdate(listener) // listener called when realm syncs async update to user info
//  start() // void
//  stop() // void
//  probe() // Promise<Boolean>
//  autoLogin() // Promise<LocalIdentity>
//  confirmUser(userInfo) // Promise<LocalIdentity>
//  signOut() // Promise<?>
//  signIn() // Promise<LocalIdentity>

const log = logFactory("IdentityStore");

export function buildDefaultIdentityStore() {
    const identityStore = new IdentityStore();
    identityStore.addRealmProvider("GOOGLE", new GoogleIdentityProvider());
    identityStore.addRealmProvider("FACEBOOK", new FacebookIdentityProvider());
    return identityStore;
}

export default class IdentityStore {
    constructor() {
        this._realmProviders = {};
        this._realmsAvailable = new Set();
        this._subscribers = new Set();
        this.localUserIdentity = null;
    }

    addRealmProvider(name, provider) {
        this._realmProviders[name] = provider;
    }

    start() {
        for (const [name, provider] of Object.entries(this._realmProviders)) {
            provider.onAvailable(() => {
                this._realmsAvailable.add(name);
                this._emit("realmAvailable", name);
            });
            provider.onUserUpdate(userInfo => this._emit("localUserIdentity", userInfo));
            provider.start();
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
        for (const [name, provider] in Object.entries(this._realmProviders)) {
            if (result[name]) {
                return provider.autoLogin();
            }
        }
        return null;
    }

    realmAvailable(realmName) {
        return this._realmsAvailable.has(realmName);
    }

    signInWith(realmName) {
        if (!this.realmAvailable(realmName))
            throw new Error(`Realm not available: ${realmName}`);
        return this._signIn(this._realmProviders[realmName]);
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
