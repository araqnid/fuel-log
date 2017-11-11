import {combineReducers} from "redux";
import {reducer as identity} from "./stores/IdentityStore";
import {reducer as preferences} from "./stores/PreferencesStore";
import {reducer as purchases} from "./stores/PurchasesStore";

export default combineReducers({
    identity,
    preferences,
    purchases
});
