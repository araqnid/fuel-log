import axios from "axios";
import {logFactory} from "../util/ConsoleLog";

const log = logFactory("GoogleIdentityProvider");

export default class GoogleIdentityProvider {
    constructor() {
        this._available = false;
        this._availableListeners = [];
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
                }
                else {
                    log.info("Using existing GoogleAuth instance");
                }
            })
            .then(() => {
                log.info("GoogleAuth initialised");
                this._markAvailable();
            }, () => {
                log.info("GoogleAuth initialisation failed");
            });
    }

    start() {
    }

    stop() {
    }

    onAvailable(listener) {
        if (this._available)
            listener();
        else
            this._availableListeners.push(listener);
    }

    _markAvailable() {
        this._available = true;
        this._availableListeners.forEach(listener => {
            listener();
        });
    }

    probe() {
        return this._authInitialised.then(() => {
            log.info("start probing for Google user");
            return this._currentUser();
        }).then(googleUser => {
            log.info("probed", googleUser);
            return !!googleUser.getId();
        });
    }

    autoLogin() {
        log.info("Go ahead with auto-login");
        const currentUser = this._googleAuth.currentUser.get();
        const idToken = currentUser.getAuthResponse().id_token;
        if (idToken) {
            log.info("already have an ID token");
            return this._associate(idToken);
        }
        else {
            return Promise.reject("need to sign in to get ID token");
        }
    }

    signIn() {
        log.info("Sign in");
        return this._authInitialised.then(() => {
            const currentUser = this._googleAuth.currentUser.get();
            const idToken = currentUser.getAuthResponse().id_token;
            if (idToken) {
                log.info("already have an ID token");
                return currentUser;
            }
            else {
                log.info("need to sign in to get ID token");
                return this._googleAuth.signIn();
            }
        }).then(googleUser => {
            log.info("resolved Google user for sign-in", googleUser);
            return this._associate(googleUser.getAuthResponse().id_token);
        });
    }

    signOut() {
        log.info("Sign out");
        return this._authInitialised
            .then(() => {
                this._googleAuth.signOut();
            });
    }

    confirmUser(userInfo) {
        return this._authInitialised
            .then(() => {
                log.info("confirm Google user", userInfo);
                return this._currentUser();
            })
            .then(googleUser => {
                if (!googleUser) return null;
                const idToken = googleUser.getAuthResponse().id_token;
                if (idToken) {
                    return this._associate(idToken);
                }
                else {
                    return null;
                }
            });
    }

    _associate(idToken) {
        return axios.post('/_api/user/identity/google', idToken, { headers: { "Content-Type": "text/plain" } })
            .then(({data}) => data);
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
}
