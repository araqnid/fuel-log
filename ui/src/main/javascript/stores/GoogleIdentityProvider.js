import axios from "axios";
import {logFactory} from "../util/ConsoleLog";

const log = logFactory("GoogleIdentityProvider");

export default class GoogleIdentityProvider {
    constructor() {
        this._available = false;
        this._availableListeners = [];
        this._forwardUserUpdatesEnabled = false;
        this._forwardUserUpdates = null;
        this._authInitialised = __api_hooks.googleApi.promise
            .then(() => new Promise(resolve => {
                log.info("start loading auth2");
                gapi.load('auth2', () => {
                    log.info("auth2 loaded");
                    resolve();
                });
            }))
            .then(() => {
                this._googleAuth = gapi.auth2.getAuthInstance();
                if (this._googleAuth === null) {
                    log.info("Initialising GoogleAuth instance");
                    this._googleAuth = gapi.auth2.init({});
                    this._googleAuth.currentUser.listen(this._currentUserUpdated.bind(this));
                }
                else {
                    log.info("Using existing GoogleAuth instance");
                }
                this._markAvailable();
                return null;
            })
    }

    start() {
    }

    stop() {
        // can't unlisten to GoogleAuth api, but realistically this is just a hot-reload dev-mode problem
        this._forwardUserUpdates = null;
    }

    onAvailable(listener) {
        if (this._available)
            listener();
        else
            this._availableListeners.push(listener);
    }

    onUserUpdate(listener) {
        this._forwardUserUpdates = listener;
    }

    _markAvailable() {
        this._available = true;
        this._availableListeners.forEach(listener => {
            listener();
        });
    }

    async probe() {
        await this._authInitialised;

        log.info("start probing for Google user");
        const googleUser = await this._currentUser();

        log.info("probed", googleUser);
        return !!googleUser.getId();
    }

    async autoLogin() {
        log.info("Go ahead with auto-login");
        const currentUser = this._googleAuth.currentUser.get();
        const idToken = currentUser.getAuthResponse().id_token;
        if (idToken) {
            log.info("already have an ID token");
            return this._associate(idToken);
        }
        else {
            throw new Error("need to sign in to get ID token");
        }
    }

    async signIn() {
        log.info("Sign in");
        await this._authInitialised;

        let googleUser;
        const currentUser = this._googleAuth.currentUser.get();
        const idToken = currentUser.getAuthResponse().id_token;
        if (idToken) {
            log.info("already have an ID token");
            googleUser = currentUser;
        }
        else {
            log.info("need to sign in to get ID token");
            googleUser = await this._googleAuth.signIn();
        }

        log.info("resolved Google user for sign-in", googleUser);
        return this._associate(googleUser.getAuthResponse().id_token);
    }

    async signOut() {
        log.info("Sign out");
        await this._authInitialised;
        this._enableUserForwarding(false);
        this._googleAuth.signOut();
    }

    async confirmUser(userInfo) {
        await this._authInitialised;

        log.info("confirm Google user", userInfo);
        const googleUser = await this._currentUser();
        if (!googleUser) return null;

        const idToken = googleUser.getAuthResponse().id_token;
        if (idToken) {
            return this._associate(idToken);
        }
        else {
            return null;
        }
    }

    async _associate(idToken) {
        const { data: userInfo } = await axios.post('/_api/user/identity/google', idToken, { headers: { "Content-Type": "text/plain" } });
        this._enableUserForwarding();
        return userInfo;
    }

    _currentUser() {
        return new Promise((resolve, reject) => {
            this._googleAuth.then(v => {
                log.info("_googleAuth finished initialising", v);
                const user = v.currentUser.get();
                log.info("produced current user", user);
                resolve(user);
            }, err => {
                log.info("_googleAuth threw error while initialising", err);
                reject(err);
            });
        });
    }

    _enableUserForwarding(enabled = true) {
        log.info((enabled ? "enabling" : "disabling") + " user update forwarding");
        this._forwardUserUpdatesEnabled = enabled;
    }

    _currentUserUpdated(googleUser) {
        const forwardTo = this._forwardUserUpdates;
        if (forwardTo && this._forwardUserUpdatesEnabled) {
            const idToken = googleUser.getAuthResponse().id_token;
            if (idToken) {
                log.info("forwarding currentUser update", googleUser);
                this._associate(idToken).then(forwardTo);
            }
            else {
                log.info("currentUser updated and dropped id token");
                forwardTo(null);
            }
        }
        else {
            log.info("ignoring currentUser update", googleUser);
        }
    }
}
