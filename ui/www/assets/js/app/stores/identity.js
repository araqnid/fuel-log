import $ from "jquery";
import MemoBus from "app/MemoBus";

class Identity {
    constructor() {
        this.bus = new MemoBus("Identity");
        this.gapiLoaded = false;
        this.fbsdkLoaded = false;
        this.registering = null;
        this.facebookProfile = null;
        this.realm = null;
        this._pendingSignInRealm = null;
        this._autoLoginRealms = {};
    }
    start() {
        BUS.subscribe('GoogleApi.Loaded', () => {
            if (!this.gapiLoaded) {
                this._doStartGoogle();
            }
        });
        if ('gapi' in window) {
            this._doStartGoogle();
        }

        BUS.subscribe('FacebookSdk.Loaded', () => {
            if (!this.fbsdkLoaded) {
                this._doStartFacebook();
            }
        });
        if ('FB' in window) {
            this._doStartFacebook();
        }

        this._beginFetchLocalIdentity();

        BUS.subscribe('GoogleApi.Auth2.CurrentUser', googleUser => {
            this.bus.dispatch('googleUser', googleUser);
            this.googleUser = googleUser;
            const idToken = googleUser.getAuthResponse().id_token;
            if (idToken) {
                if (this._pendingSignInRealm === "GOOGLE") {
                    this._pendingSignInRealm = null;
                    this._doAssociateGoogle(idToken);
                }
                else {
                    this._joinForAutoLogin("GOOGLE");
                }
            }
            else {
                if (this._pendingSignInRealm === "GOOGLE") {
                    this._pendingSignInRealm = null;
                    this._clearLocalIdentity();
                }
                else {
                    this._joinForAutoLogin("GOOGLE");
                }
            }
        });
        BUS.subscribe('GoogleApi.Auth2.IsSignedIn', signedIn => {
            this.bus.dispatch('googleUserSignedIn', signedIn);
        });

        BUS.subscribe('FacebookSdk.LoginStatus', facebookStatus => {
            this.bus.dispatch('facebookLoginStatus', facebookStatus);
            if (facebookStatus.status === 'connected') {
                this.facebookProfile = { authResponse: facebookStatus.authResponse };
                FB.api('/me', response => {
                    BUS.broadcast('FacebookSdk.Me', response);
                });
                FB.api('/me/picture', response => {
                    BUS.broadcast('FacebookSdk.Me.Picture', response);
                });
            }
            else {
                if (this._pendingSignInRealm === "FACEBOOK") {
                    this._pendingSignInRealm = null;
                    this._clearLocalIdentity();
                }
                else {
                    this._joinForAutoLogin("FACEBOOK");
                }
                this.facebookProfile = null;
                this.bus.dispatch('facebookProfile', null);
            }
        });

        BUS.subscribe('FacebookSdk.Me', facebookUser => {
            this.facebookProfile.user = facebookUser;
            this._joinFacebookProfile();
        });
        BUS.subscribe('FacebookSdk.Me.Picture', facebookPicture => {
            this.facebookProfile.picture = facebookPicture;
            this._joinFacebookProfile();
        });
    }
    beginGoogleSignIn() {
        let authResponse = this.googleUser.getAuthResponse();
        const idToken = authResponse.id_token;
        if (idToken) {
            // already signed in
            this._doAssociateGoogle(idToken);
        }
        else {
            gapi.auth2.getAuthInstance().signIn();
            this._pendingSignInRealm = "GOOGLE";
        }
    }
    beginGoogleSignOut() {
        gapi.auth2.getAuthInstance().signOut();
    }
    beginFacebookSignIn() {
        if (this.facebookProfile) {
            this._doAssociateFacebook();
        }
        else {
            this._pendingSignInRealm = "FACEBOOK";
            FB.login(response => {
                BUS.broadcast("FacebookSdk.LoginStatus", response);
            });
        }
    }
    beginFacebookSignOut() {
        FB.logout(response => {
            BUS.broadcast("FacebookSdk.LoginStatus", response);
        });
    }
    beginSignOut() {
        if (this.realm === "GOOGLE") {
            this.beginGoogleSignOut();
        }
        else if (this.realm === "FACEBOOK") {
            this.beginFacebookSignOut();
        }
        $.ajax({
            url: '/_api/user/identity',
            method: 'DELETE',
            success: (data, status, xhr) => {
                this._clearLocalIdentity();
            },
            error: (xhr, status, ex) => {
                this.bus.dispatch('loadingError', ex);
            },
            complete: (xhr, status) => {
            }
        });
    }
    subscribe(handlers, owner) {
        this.bus.subscribeAll(handlers, owner);
    }
    unsubscribe(owner) {
        this.bus.unsubscribe(owner);
    }
    _joinFacebookProfile() {
        if (!this.facebookProfile.user || !this.facebookProfile.picture) return;
        this.bus.dispatch('facebookProfile', this.facebookProfile);
        if (this._pendingSignInRealm === "FACEBOOK") {
            this._pendingSignInRealm = null;
            this._doAssociateFacebook();
        }
        else {
            this._joinForAutoLogin("FACEBOOK");
        }
    }
    _joinForAutoLogin(realm) {
        if (realm === "GOOGLE") {
            this._autoLoginRealms.google = this.googleUser.getAuthResponse().id_token !== null;
        }
        else if (realm === "FACEBOOK") {
            this._autoLoginRealms.facebook = this.facebookProfile !== null;
        }
        if (Object.keys(this._autoLoginRealms).length !== 2) return;
        console.log("auto-login", this._autoLoginRealms);
        if (this._autoLoginRealms.google) {
            this.beginGoogleSignIn();
        }
        else if (this._autoLoginRealms.facebook) {
            this.beginFacebookSignIn();
        }
    }
    _doStartGoogle() {
        this.gapiLoaded = true;
        gapi.load('auth2', function() {
            let auth2 = gapi.auth2.getAuthInstance();
            if (auth2 === null) {
                auth2 = gapi.auth2.init({
                    // client_id: '515812716745-t7hno1i869lv1fc127j36r3shcgfr76g.apps.googleusercontent.com',
                    // fetch_basic_profile: false,
                    // scope: 'profile openid'
                });
            }
            auth2.isSignedIn.listen(v => {
                BUS.broadcast("GoogleApi.Auth2.IsSignedIn", v);
            });
            auth2.currentUser.listen(v => {
                BUS.broadcast("GoogleApi.Auth2.CurrentUser", v);
            })
        });
    }
    _doStartFacebook() {
        this.fbsdkLoaded = true;
        FB.getLoginStatus(response => {
            BUS.broadcast("FacebookSdk.LoginStatus", response);
        });
    }
    _doAssociateGoogle(idToken) {
        if (this.registering) {
            this.registering.abort();
        }
        this.registering = $.ajax({
            headers: { accept: 'application/json' },
            url: '/_api/user/identity/google',
            method: 'POST',
            contentType: 'text/plain',
            data: idToken,
            success: (data, status, xhr) => {
                this._storeLocalIdentity(data);
            },
            error: (xhr, status, ex) => {
                this.bus.dispatch('loadingError', ex);
            },
            complete: (xhr, status) => {
                this.registering = null;
            }
        })
    }
    _doAssociateFacebook() {
        if (this.registering) {
            this.registering.abort();
        }
        this.registering = $.ajax({
            headers: { accept: 'application/json' },
            url: '/_api/user/identity/facebook',
            method: 'POST',
            data: { id: this.facebookProfile.user.id, name: this.facebookProfile.user.name, picture: this.facebookProfile.picture.data.url },
            success: (data, status, xhr) => {
                this._storeLocalIdentity(data);
            },
            error: (xhr, status, ex) => {
                this.bus.dispatch('loadingError', ex);
            },
            complete: (xhr, status) => {
                this.registering = null;
            }
        });
    }
    _beginFetchLocalIdentity() {
        $.ajax({
            headers: { accept: 'application/json' },
            url: '/_api/user/identity',
            method: 'GET',
            success: (data, status, xhr) => {
                if (data.user_info) {
                    if (data.user_info.realm === "GOOGLE") {
                        if (this.googleUser) { // either full or empty
                            this.beginGoogleSignIn();
                        }
                        else {
                            this._pendingSignInRealm = "GOOGLE";
                        }
                    }
                    else if (data.user_info.realm === "FACEBOOK") {
                        if (this.fbsdkLoaded) {
                            this.beginFacebookSignIn();
                        }
                        else {
                            this._pendingSignInRealm = "FACEBOOK";
                        }
                    }
                    else {
                        this._storeLocalIdentity(data);
                    }
                }
                else {
                    this._clearLocalIdentity();
                }
            },
            error: (xhr, status, ex) => {
                this.bus.dispatch('loadingError', status, ex);
            },
            complete: (xhr, status) => {
            }
        });
    }
    _storeLocalIdentity(data) {
        const localIdentity = { userId: data.user_id, name: data.name, picture: data.picture, realm: data.realm };
        this.bus.dispatch('localIdentity', localIdentity);
        this.realm = data.realm;
    }
    _clearLocalIdentity() {
        this.bus.dispatch('localIdentity', null);
        this.realm = null;
    }
}

export default new Identity();

/*
 start -> identity_loaded(full)
       -> identity_loaded(none)

 identity_loaded(none) ; beginGoogleSignIn -> loading_google -> linking_google -> identity_loaded(full)
 identity_loaded(full) && realm == Google ; beginGoogleSignOut -> identity_loaded(none)
 identity_loaded(full) && realm == Facebook ; beginFacebookSignOut -> identity_loaded(none)
 */