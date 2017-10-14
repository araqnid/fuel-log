import {Datum, StoreBase} from "../util/Stores";
import {logFactory} from "../util/ConsoleLog";
import BUS from "../message-bus";

const log = logFactory("GoogleIdentityProvider");

export default class GoogleIdentityProvider extends StoreBase {
    constructor() {
        super();
        this._available = new Datum(this, "available");
        let apiLoaded;
        if ('gapi' in window) {
            log.info("Google API loaded early");
            apiLoaded = Promise.resolve(true);
        }
        else {
            apiLoaded = new Promise(resolve => {
                BUS.subscribe('GoogleApi.Loaded', () => {
                    log.info("Google API loaded late");
                    resolve(true);
                });
            });
        }
        const authLoaded = new Promise(resolve => {
            apiLoaded.then(() => {
                log.info("start loading auth2");
                gapi.load('auth2', () => {
                    log.info("auth2 loaded");
                    resolve();
                });
            });
        });
        const authInitStarted = authLoaded.then(() => {
            this._googleAuth = gapi.auth2.getAuthInstance();
            if (this._googleAuth === null) {
                log.info("Initialising GoogleAuth instance");
                this._googleAuth = gapi.auth2.init({});
            }
            else {
                log.info("Using existing GoogleAuth instance");
            }
        });
        this._authInitialised = authInitStarted.then(() => {
            log.info("GoogleAuth initialised");
            this._available.value = true;
        }, () => {
            log.info("GoogleAuth initialisation failed");
        });
    }

    get available() {
        return this._available.value;
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
        return this.callAjax({
            url: '/_api/user/identity/google',
            type: 'POST',
            contentType: 'text/plain',
            data: idToken
        });
    }

    _currentUser() {
        return new Promise(resolve => {
            this._googleAuth.currentUser.listen(v => {
                log.info("currentUser <=", v);
                resolve(v);
            });
        });
    }
}
